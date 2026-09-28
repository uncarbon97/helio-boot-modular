package cc.uncarbon.test.file;

import cc.uncarbon.framework.helium.db.enums.YesOrNoEnum;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.module.file.enums.StoragePlatformTypeEnum;
import cc.uncarbon.module.file.facade.FileUpDownloadFacade;
import cc.uncarbon.module.file.model.internal.FacadeUploadOptions;
import cc.uncarbon.module.file.model.request.AdminFileStorageUpsertRequest;
import cc.uncarbon.module.file.model.request.FileAttrExtraRequest;
import cc.uncarbon.module.file.model.response.FileDownloadReply;
import cc.uncarbon.module.file.model.valueobj.FileMetaDTO;
import cc.uncarbon.module.file.service.FileMetaService;
import cc.uncarbon.module.file.service.FileStorageDataService;
import cc.uncarbon.module.tenant.facade.TenantFacade;
import cc.uncarbon.module.tenant.model.request.AdminTenantCreateRequest;
import cc.uncarbon.module.tenant.service.TenantService;
import cc.uncarbon.test.base.BaseIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import cc.uncarbon.module.file.dal.mapper.FileMetaMapper;
import cc.uncarbon.module.file.dal.mapper.FileStorageMapper;
import cc.uncarbon.module.tenant.dal.mapper.TenantMetaMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.dal.mapper.SysRoleMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserRoleRelationMapper;
import cc.uncarbon.module.sys.dal.mapper.SysRoleMenuRelationMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserTenantRelationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserTenantRelationEntity;

/**
 * 文件上传下载集成测试：动态注册 DB 存储点 → 上传 → SHA256 秒传索引 → 服务端代理下载
 * 多租户启用时在独立租户视角执行；未启用时平台态执行
 */
@Tag("file")
class FileUpDownloadIT extends BaseIntegrationTest {

    private static final byte[] CONTENT = "hello helium it".getBytes();

    @Resource
    private FileUpDownloadFacade fileUpDownloadFacade;
    @Resource
    private FileStorageDataService fileStorageDataService;
    @Resource
    private FileMetaService fileMetaService;
    @Resource
    private TenantService tenantService;
    @Resource
    private TenantFacade tenantFacade;

    @Resource
    private FileMetaMapper fileMetaMapper;
    @Resource
    private FileStorageMapper fileStorageMapper;
    @Resource
    private TenantMetaMapper tenantMetaMapper;
    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private SysRoleMapper sysRoleMapper;
    @Resource
    private SysUserRoleRelationMapper userRoleRelationMapper;
    @Resource
    private SysRoleMenuRelationMapper roleMenuRelationMapper;
    @Resource
    private SysUserTenantRelationMapper userTenantRelationMapper;

    private Long tenantId;
    private String tenantCode;
    private Long storageId;
    private Long fileMetaId;


    @Test
    void uploadIndexAndDownload() throws Exception {
        String nano = String.valueOf(System.nanoTime() % 1_000_000_000L);
        boolean tenantEnabled = tenantFacade.isTenantEnabled();
        if (tenantEnabled) {
            tenantCode = "itfs" + nano;
            tenantId = tenantService.adminCreate(new AdminTenantCreateRequest()
                    .setCode(tenantCode).setName("IT文件租户")
                    .setTenantAdminPin("itfsa" + nano).setTenantAdminPwd("it-pass-123456")
                    .setTenantAdminEmail("it@a.b.c").setTenantAdminPhoneNo("13800001111"));
        }

        String digest = sha256Hex(CONTENT);
        var ref = new Object() {
            FileMetaDTO meta;
        };

        Runnable scenario = () -> {
            // 1. 动态注册 DB 存储点（主存储点，本地盘）
            Map<String, Object> settingBody = new HashMap<>();
            settingBody.put("basePath", "/");
            settingBody.put("storagePath", "./target/it-file-storage");
            settingBody.put("domain", "");
            storageId = fileStorageDataService.adminCreate(new AdminFileStorageUpsertRequest()
                    .setCode("itst" + nano).setName("IT存储点")
                    .setPlatformType(StoragePlatformTypeEnum.LOCAL)
                    .setSettingBody(settingBody).setPrimaryFlag(YesOrNoEnum.YES));
            Assertions.assertNotNull(storageId);

            // 2. 上传（未指定平台 → 主存储点）
            ref.meta = fileUpDownloadFacade.upload(CONTENT,
                    new FacadeUploadOptions()
                            .setOriginalFilename("it.txt").setContentType("text/plain")
                            .setDigestSha256(digest),
                    new FileAttrExtraRequest().setCategory("it"));
            Assertions.assertNotNull(ref.meta.getId());
            Assertions.assertEquals("it.txt", ref.meta.getOriginalFilenameFull());
            Assertions.assertEquals((long) CONTENT.length, ref.meta.getFileSize());
        };
        if (tenantEnabled) {
            withContext(testUser(0L, "admin"),
                    testTenant(tenantId, "IT文件租户", tenantCode), scenario);
        } else {
            scenario.run();
        }
        fileMetaId = ref.meta.getId();

        // 3. SHA256 秒传索引
        FileMetaDTO indexed = fileUpDownloadFacade.findByDigestSha256(digest);
        Assertions.assertNotNull(indexed);
        Assertions.assertEquals(fileMetaId, indexed.getId());
        Assertions.assertNull(fileUpDownloadFacade.findByDigestSha256("not-exist-digest"));

        // 4. 服务端代理下载（本地存储无直链 → byte[]）
        FileDownloadReply reply = fileUpDownloadFacade.downloadByTenantAndId(
                tenantEnabled ? tenantCode : null, fileMetaId);
        Assertions.assertTrue(reply.isSuccess(), "下载应成功，错误码=" + reply.getErrorCode());
        Assertions.assertFalse(reply.isRedirect2DirectUrl());
        Assertions.assertArrayEquals(CONTENT, reply.getFileBytes());
        Assertions.assertTrue(reply.getStorageFilename().endsWith(".txt"));

        // 5. 不存在的文件 → 错误码而非异常
        FileDownloadReply missing = fileUpDownloadFacade.downloadByTenantAndId(
                tenantEnabled ? tenantCode : null, 99_999_999L);
        Assertions.assertFalse(missing.isSuccess());
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private static String sha256Hex(byte[] content) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(md.digest(content));
    }

    @AfterEach
    void cleanup() {
        TenantContextHolder.runIgnored(() -> {
            // 文件元数据（尽力删物理文件）
            if (fileMetaId != null) {
                try {
                    fileMetaService.adminDelete(List.of(fileMetaId));
                } catch (Exception ignored) {
                    fileMetaMapper.deleteById(fileMetaId);
                }
            }
            // 存储点（无引用后可删；主存储点保护跳过时兜底直删）
            if (storageId != null) {
                try {
                    fileStorageDataService.adminDelete(List.of(storageId));
                } catch (Exception ignored) {
                    fileStorageMapper.deleteById(storageId);
                }
            }
            // 租户与随建数据
            if (tenantId != null) {
                var roleIds = sysRoleMapper.selectList(
                                new LambdaQueryWrapper<SysRoleEntity>()
                                        .eq(SysRoleEntity::getTenantId, tenantId))
                        .stream().map(SysRoleEntity::getId).toList();
                if (!roleIds.isEmpty()) {
                    roleMenuRelationMapper.deleteByRoleIds(roleIds);
                    userRoleRelationMapper.deleteByRoleIds(roleIds);
                    sysRoleMapper.deleteByIds(roleIds);
                }
                var userIds = userTenantRelationMapper.selectList(
                                new LambdaQueryWrapper<SysUserTenantRelationEntity>()
                                        .eq(SysUserTenantRelationEntity::getTenantId, tenantId))
                        .stream().map(SysUserTenantRelationEntity::getUserId).toList();
                if (!userIds.isEmpty()) {
                    userTenantRelationMapper.delete(
                            new LambdaQueryWrapper<SysUserTenantRelationEntity>()
                                    .eq(SysUserTenantRelationEntity::getTenantId, tenantId));
                    sysUserMapper.deleteByIds(userIds);
                }
                tenantMetaMapper.deleteById(tenantId);
            }
        });
    }
}
