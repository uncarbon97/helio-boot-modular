package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.page.PageParam;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.web.context.SimpleVisitorContext;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.commons.iplocation.IPLocationResolver;
import cc.uncarbon.module.sys.MybatisPlusTestSupport;
import cc.uncarbon.module.sys.dal.entity.SysLoginLogEntity;
import cc.uncarbon.module.sys.dal.mapper.SysLoginLogMapper;
import cc.uncarbon.module.sys.enums.LogResultStatusEnum;
import cc.uncarbon.module.sys.enums.LoginLogTypeEnum;
import cc.uncarbon.module.sys.model.query.AdminSysLoginLogListQuery;
import cc.uncarbon.module.sys.model.request.SysLoginLogCreateRequest;
import cc.uncarbon.module.sys.model.valueobj.SysLoginLogDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

/**
 * {@link SysLoginLogServiceImpl} 查询条件分支、UA 截断、IP 归属地解析与租户ID透传测试
 */
@ExtendWith(MockitoExtension.class)
class SysLoginLogServiceImplTest {

    private static final String CHROME_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    @Mock
    private SysLoginLogMapper sysLoginLogMapper;
    @Mock
    private IPLocationResolver primaryResolver;
    @Mock
    private IPLocationResolver fallbackResolver;
    private SysLoginLogServiceImpl service;

    @BeforeAll
    static void initTableInfoCache() {
        MybatisPlusTestSupport.initTableInfo();
    }

    @BeforeEach
    void setUp() {
        // 构造器含 List<IPLocationResolver>，@InjectMocks 无法按泛型注入，参照 SysDictServiceImplTest 手动构造
        service = new SysLoginLogServiceImpl(sysLoginLogMapper, List.of(primaryResolver, fallbackResolver));
    }


    /*
    ----------------------------------------------------------------
                        后台管理-分页查询
    ----------------------------------------------------------------
     */

    @Test
    void adminListAppliesAllConditions() {
        Mockito.when(sysLoginLogMapper.selectPage(Mockito.any(), Mockito.any())).thenAnswer(inv -> {
            Page<SysLoginLogEntity> page = inv.getArgument(0);
            page.setRecords(List.of(entity()));
            page.setTotal(1);
            return page;
        });

        PageResult<SysLoginLogDTO> ret = service.adminList(new AdminSysLoginLogListQuery()
                .setPageParam(new PageParam(1, 10))
                .setUserPin("u1")
                .setResultStatus(LogResultStatusEnum.SUCCESS)
                .setBeginAt(Instant.parse("2026-01-01T00:00:00Z"))
                .setEndAt(Instant.parse("2026-02-01T00:00:00Z")));

        Assertions.assertEquals(1, ret.getTotal());
        Assertions.assertEquals(1, ret.getRecords().size());
        SysLoginLogDTO dto = ret.getRecords().getFirst();
        // 实体 UA 被解析为浏览器 + 操作系统描述
        Assertions.assertEquals("Chrome 120.0.0.0", dto.getVisitorBrowser());
        Assertions.assertEquals("Windows 10 or Windows Server 2016 10.0", dto.getVisitorOs());

        // 传入全部条件时，查询片段应包含 账号模糊、状态等值、时间区间
        @SuppressWarnings("unchecked")
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysLoginLogEntity>> wrapperCaptor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        Mockito.verify(sysLoginLogMapper).selectPage(Mockito.any(), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        Assertions.assertTrue(sqlSegment.contains("user_pin LIKE"));
        Assertions.assertTrue(sqlSegment.contains("result_status ="));
        Assertions.assertTrue(sqlSegment.contains("created_at BETWEEN"));
    }

    @Test
    void adminListWithoutConditionsSkipsFilters() {
        Mockito.when(sysLoginLogMapper.selectPage(Mockito.any(), Mockito.any())).thenAnswer(inv -> {
            Page<SysLoginLogEntity> page = inv.getArgument(0);
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        });

        PageResult<SysLoginLogDTO> ret = service.adminList(new AdminSysLoginLogListQuery()
                .setPageParam(new PageParam(1, 10)));

        // 空结果走 convertList 空集合分支
        Assertions.assertEquals(0, ret.getTotal());
        Assertions.assertTrue(ret.getRecords().isEmpty());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysLoginLogEntity>> wrapperCaptor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        Mockito.verify(sysLoginLogMapper).selectPage(Mockito.any(), wrapperCaptor.capture());
        // 仅剩排序，不含任何过滤条件
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        Assertions.assertFalse(sqlSegment.contains("LIKE"));
        Assertions.assertFalse(sqlSegment.contains("BETWEEN"));
        Assertions.assertTrue(sqlSegment.contains("ORDER BY"));
    }


    /*
    ----------------------------------------------------------------
                        新增
    ----------------------------------------------------------------
     */

    @Test
    void createTruncatesOverlongUserAgent() {
        Mockito.when(sysLoginLogMapper.insert(Mockito.any(SysLoginLogEntity.class))).thenAnswer(inv -> {
            inv.<SysLoginLogEntity>getArgument(0).setId(66L);
            return 1;
        });
        Mockito.when(primaryResolver.resolve("8.8.8.8")).thenReturn("中国|0|北京市|北京市|联通");

        String overlongUa = "M".repeat(SysLoginLogServiceImpl.USER_AGENT_MAX_LENGTH + 10);
        Long id = service.create(request(overlongUa, "8.8.8.8", 5L));

        Assertions.assertEquals(66L, id);
        ArgumentCaptor<SysLoginLogEntity> captor = ArgumentCaptor.forClass(SysLoginLogEntity.class);
        Mockito.verify(sysLoginLogMapper).insert(captor.capture());
        SysLoginLogEntity saved = captor.getValue();
        // 超长度 UA 截断到上限
        Assertions.assertEquals(SysLoginLogServiceImpl.USER_AGENT_MAX_LENGTH, saved.getVisitorUserAgent().length());
        Assertions.assertEquals("8.8.8.8", saved.getVisitorIp());
        Assertions.assertEquals("中国|0|北京市|北京市|联通", saved.getVisitorIpLocation());
        // TENANT_FIRST 场景：登录成功时租户ID原样透传
        Assertions.assertEquals(5L, saved.getTenantId());
        Assertions.assertEquals("u1", saved.getUserPin());
    }

    @Test
    void createFallsThroughResolversUntilHit() {
        Mockito.when(sysLoginLogMapper.insert(Mockito.any(SysLoginLogEntity.class))).thenReturn(1);
        // 首选解析器返回 null，继续尝试兜底解析器
        Mockito.when(primaryResolver.resolve("1.2.3.4")).thenReturn(null);
        Mockito.when(fallbackResolver.resolve("1.2.3.4")).thenReturn("内网");

        service.create(request("UA", "1.2.3.4", 0L));

        ArgumentCaptor<SysLoginLogEntity> captor = ArgumentCaptor.forClass(SysLoginLogEntity.class);
        Mockito.verify(sysLoginLogMapper).insert(captor.capture());
        Assertions.assertEquals("内网", captor.getValue().getVisitorIpLocation());
        // USER_FIRST 无租户上下文场景：tenantId=0（平台自营域）原样落库
        Assertions.assertEquals(0L, captor.getValue().getTenantId());
    }

    @Test
    void createKeepsNullIpLocationWhenNoResolverHit() {
        Mockito.when(sysLoginLogMapper.insert(Mockito.any(SysLoginLogEntity.class))).thenReturn(1);
        Mockito.when(primaryResolver.resolve("1.2.3.4")).thenReturn(null);
        Mockito.when(fallbackResolver.resolve("1.2.3.4")).thenReturn(null);

        service.create(request("UA", "1.2.3.4", 5L));

        ArgumentCaptor<SysLoginLogEntity> captor = ArgumentCaptor.forClass(SysLoginLogEntity.class);
        Mockito.verify(sysLoginLogMapper).insert(captor.capture());
        // 所有解析器均未命中 → 归属地保持 null
        Assertions.assertNull(captor.getValue().getVisitorIpLocation());
    }

    @Test
    void createSkipsResolversWhenClientIpNull() {
        Mockito.when(sysLoginLogMapper.insert(Mockito.any(SysLoginLogEntity.class))).thenReturn(1);

        service.create(request("UA", null, 5L));

        // clientIp 为 null → 不触发任何解析器
        Mockito.verifyNoInteractions(primaryResolver, fallbackResolver);
        ArgumentCaptor<SysLoginLogEntity> captor = ArgumentCaptor.forClass(SysLoginLogEntity.class);
        Mockito.verify(sysLoginLogMapper).insert(captor.capture());
        Assertions.assertNull(captor.getValue().getVisitorIp());
        Assertions.assertNull(captor.getValue().getVisitorIpLocation());
        Assertions.assertEquals("UA", captor.getValue().getVisitorUserAgent());
    }


    /*
    ----------------------------------------------------------------
                        单条查询
    ----------------------------------------------------------------
     */

    @Test
    void getByIdNullIdReturnsNullWithoutQuery() {
        Assertions.assertNull(service.getById(null));
        Mockito.verifyNoInteractions(sysLoginLogMapper);
    }

    @Test
    void getByIdMissingReturnsNull() {
        Mockito.when(sysLoginLogMapper.selectById(9L)).thenReturn(null);

        Assertions.assertNull(service.getById(9L));
    }

    @Test
    void getByIdParsesUserAgent() {
        Mockito.when(sysLoginLogMapper.selectById(9L)).thenReturn(entity());

        SysLoginLogDTO ret = service.getById(9L);

        Assertions.assertEquals(9L, ret.getId());
        Assertions.assertEquals("u1", ret.getUserPin());
        Assertions.assertEquals("Chrome 120.0.0.0", ret.getVisitorBrowser());
        Assertions.assertEquals("Windows 10 or Windows Server 2016 10.0", ret.getVisitorOs());
    }

    @Test
    void getByIdBlankUserAgentSkipsParsingWithoutNpe() {
        SysLoginLogEntity blankUa = entity().setVisitorUserAgent(null);

        Mockito.when(sysLoginLogMapper.selectById(9L)).thenReturn(blankUa);

        SysLoginLogDTO ret = service.getById(9L);

        Assertions.assertEquals(9L, ret.getId());
        Assertions.assertNull(ret.getVisitorBrowser());
        Assertions.assertNull(ret.getVisitorOs());
    }

    @Test
    void getNonnullByIdThrowsWhenMissing() {
        Mockito.when(sysLoginLogMapper.selectById(9L)).thenReturn(null);

        Assertions.assertThrows(NoRecordException.class, () -> service.getNonnullById(9L));
    }

    @Test
    void getNonnullByIdReturnsDtoWhenExists() {
        Mockito.when(sysLoginLogMapper.selectById(9L)).thenReturn(entity());

        Assertions.assertEquals(9L, service.getNonnullById(9L).getId());
    }


    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    private SysLoginLogEntity entity() {
        SysLoginLogEntity entity = new SysLoginLogEntity()
                .setId(9L)
                .setLoginLogType(LoginLogTypeEnum.PASSWORD_LOGIN)
                .setUserPin("u1")
                .setUserId(2L)
                .setVisitorIp("8.8.8.8")
                .setVisitorUserAgent(CHROME_UA)
                .setResultStatus(LogResultStatusEnum.SUCCESS);
        // setTenantId 定义于父类，链式返回父类型，需单独调用
        entity.setTenantId(5L);
        return entity;
    }

    private SysLoginLogCreateRequest request(String ua, String clientIp, Long tenantId) {
        return new SysLoginLogCreateRequest()
                .setLoginLogType(LoginLogTypeEnum.PASSWORD_LOGIN)
                .setUserPin("u1")
                .setUserId(2L)
                .setUserTypeCode("ADMIN_USER")
                .setVisitorContext(new SimpleVisitorContext().setClientIp(clientIp).setUserAgent(ua))
                .setResultStatus(LogResultStatusEnum.SUCCESS)
                .setTenantId(tenantId);
    }
}
