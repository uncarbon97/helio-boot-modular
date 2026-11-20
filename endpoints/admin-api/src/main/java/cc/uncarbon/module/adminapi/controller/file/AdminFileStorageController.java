package cc.uncarbon.module.adminapi.controller.file;


import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.bizlog.context.LogRecordContext;
import cc.uncarbon.framework.helium.bizlog.service.impl.DiffParseFunction;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.adminapi.annotation.SysOperateLog;
import cc.uncarbon.module.adminapi.helper.FileUploadResultHelper;
import cc.uncarbon.module.adminapi.model.valueobj.FileUploadResultVO;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.constant.PermPattern;
import cc.uncarbon.module.commons.model.request.IdRequest;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.file.enums.UploadFileTypeEnum;
import cc.uncarbon.module.file.errorcode.FileErrorCodeEnum;
import cc.uncarbon.module.file.facade.FileUpDownloadFacade;
import cc.uncarbon.module.file.model.internal.FacadeUploadOptions;
import cc.uncarbon.module.file.model.query.AdminFileStorageListQuery;
import cc.uncarbon.module.file.model.request.AdminFileStorageUpsertRequest;
import cc.uncarbon.module.file.model.request.FileAttrExtraRequest;
import cc.uncarbon.module.file.model.valueobj.FileMetaDTO;
import cc.uncarbon.module.file.model.valueobj.FileStorageDTO;
import cc.uncarbon.module.file.service.FileStorageDataService;
import cc.uncarbon.module.file.util.UploadFileChecker;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.crypto.digest.DigestUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Tag(name = "后台管理-" + AdminFileStorageController.BIZ_TYPE)
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/file/storage")
@SaCheckLogin(type = StpLoginType.ADMIN)
public class AdminFileStorageController {

    /**
     * 权限码前缀
     */
    private static final String PERM_PREFIX = "file:storage:";
    static final String BIZ_TYPE = "文件存储点管理";

    private final FileStorageDataService fileStorageDataService;
    private final FileUpDownloadFacade fileUpDownloadFacade;
    private final FileUploadResultHelper fileUploadResultHelper;
    private final MultipartProperties multipartProps;


    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERM_PREFIX + PermPattern.READ)
    @Operation(summary = "分页查询")
    @PostMapping(value = "/list")
    public ApiResult<PageResult<FileStorageDTO>> list(@RequestBody @Valid AdminFileStorageListQuery query) {
        return ApiResult.success(fileStorageDataService.adminList(query));
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERM_PREFIX + PermPattern.READ)
    @Operation(summary = "详情")
    @PostMapping(value = "/detail")
    public ApiResult<FileStorageDTO> detail(@RequestBody @Valid IdRequest<Long> request) {
        return ApiResult.success(fileStorageDataService.getNonnullById(request.getId()));
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "新增文件存储点", success = "新增文件存储点：{{#request.code}}|{{#request.name}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERM_PREFIX + PermPattern.CREATE)
    @Operation(summary = "新增")
    @PostMapping(value = "/create")
    public ApiResult<Void> create(@RequestBody @Valid AdminFileStorageUpsertRequest request) {
        fileStorageDataService.adminCreate(request);
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "修改文件存储点",
            bizNo = "{{#request.id}}", success = "被操作文件存储点：{{#old.code}}|{{#old.name}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERM_PREFIX + PermPattern.UPDATE)
    @Operation(summary = "修改")
    @PostMapping(value = "/update")
    public ApiResult<Void> update(@RequestBody @Valid AdminFileStorageUpsertRequest request) {
        var old = fileStorageDataService.getNonnullById(request.getId());
        fileStorageDataService.adminUpdate(request);
        // 用于操作日志
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "删除文件存储点",
            bizNo = "{{#request.id}}", success = "删除文件存储点：{{#old.code}}|{{#old.name}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERM_PREFIX + PermPattern.DELETE)
    @Operation(summary = "删除")
    @PostMapping(value = "/delete")
    public ApiResult<Void> delete(@RequestBody @Valid IdRequest<Long> request) {
        var old = fileStorageDataService.getNonnullById(request.getId());
        fileStorageDataService.adminDelete(Set.of(request.getId()));
        // 用于操作日志
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);
        return ApiResult.success();
    }

    /**
     * 安全设计：
     * 1. 复用「修改文件存储点」的权限门禁，不放宽通用上传接口 /file/upload 的任意登录即可上传限制
     * 2. 服务端生成固定小文本作为测试文件，不接收用户上传内容，规避恶意文件托管与大小/后缀风险
     * 3. 存储点按 ID 在租户上下文内查找，不暴露按编码任意指定平台
     * 4. 测试文件打上 category=storage-test 标记，便于后续批量清理
     */
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERM_PREFIX + PermPattern.UPDATE)
    @Operation(summary = "测试上传，验证存储点可用性")
    @PostMapping(value = "/test-upload")
    public ApiResult<FileUploadResultVO> testUpload(HttpServletRequest servletRequest,
                                                    @RequestBody @Valid IdRequest<Long> request) {
        var storage = fileStorageDataService.getNonnullById(request.getId());
        // 服务端生成固定小文本，内容含时间戳保证每次都会真实走一次上传
        byte[] testBytes = ("storage-test:" + storage.getCode() + ":" + System.currentTimeMillis())
                .getBytes(StandardCharsets.UTF_8);
        var options = new FacadeUploadOptions()
                .setOriginalFilename("storage-test-" + storage.getCode() + ".txt")
                .setContentType("text/plain")
                .setDigestSha256(DigestUtil.sha256Hex(testBytes))
                // 指定目标存储点
                .setPlatform(storage.getCode())
                .setUseOriginalFilenameAsDownloadFileName(false);
        FileAttrExtraRequest attr = new FileAttrExtraRequest().setCategory("storage-test");
        FileMetaDTO fileMeta = fileUpDownloadFacade.upload(testBytes, options, attr);
        // 用于操作日志
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, storage);
        return ApiResult.success(fileUploadResultHelper.toUploadResult(fileMeta, servletRequest));
    }

    /**
     * 安全设计：
     * 1. 独立权限串 file:storage:upload，与通用上传接口 /file/upload 的「登录即可上传」隔离
     * 2. 检查上传文件内容，尽力规避恶意文件托管与大小/后缀风险
     * 3. 存储点按 ID 在租户上下文内查找，不暴露按编码任意指定平台
     * 4. 上传文件打上 category=storage-test 标记，便于后续批量清理
     */
    @SysOperateLog(bizType = BIZ_TYPE, behavior = "上传文件至存储点",
            bizNo = "{{#request.id}}",
            success = "目标存储点：{{#old.code}}|{{#old.name}}，上传文件：{{#file.originalFilename}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERM_PREFIX + "upload")
    @Operation(summary = "上传自选文件至指定存储点")
    @PostMapping(value = "/upload")
    public ApiResult<FileUploadResultVO> upload(HttpServletRequest servletRequest,
                                                @RequestPart MultipartFile file,
                                                @RequestPart @Valid IdRequest<Long> request) throws IOException {
        var storage = fileStorageDataService.getNonnullById(request.getId());

        long kbMax = multipartProps.getMaxFileSize().toKilobytes();
        FileErrorCodeEnum checkResult = UploadFileChecker.check(file, kbMax);
        if (checkResult != FileErrorCodeEnum.OK) {
            throw new BusinessException(checkResult);
        }

        // 流式计算摘要；multipart 已由容器落盘，getInputStream() 每次返回新流
        String digest = DigestUtil.sha256Hex(file.getInputStream());
        UploadFileTypeEnum fileType = UploadFileTypeEnum.ofExtend(FileNameUtil.getSuffix(file.getOriginalFilename()));
        if (fileType == null) {
            // 正常不会为 null（上方 check 已按同一白名单拦截），此处兜底
            throw new BusinessException(FileErrorCodeEnum.A02004);
        }
        var options = new FacadeUploadOptions()
                .setOriginalFilename(file.getOriginalFilename())
                .setContentType(fileType.getContentType())
                .setDigestSha256(digest)
                // 指定目标存储点；不做摘要去重，确保真实写入该存储点
                .setPlatform(storage.getCode())
                .setUseOriginalFilenameAsDownloadFileName(false);
        FileAttrExtraRequest attr = new FileAttrExtraRequest().setCategory("storage-test");
        FileMetaDTO fileMeta = fileUpDownloadFacade.upload(file.getInputStream(), file.getSize(), options, attr);
        // 用于操作日志
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, storage);
        return ApiResult.success(fileUploadResultHelper.toUploadResult(fileMeta, servletRequest));
    }

}
