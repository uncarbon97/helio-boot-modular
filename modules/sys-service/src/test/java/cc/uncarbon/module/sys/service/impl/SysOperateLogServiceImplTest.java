package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.page.PageParam;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.commons.iplocation.IPLocationResolver;
import cc.uncarbon.module.sys.AuditNicknameFillerTestSupport;
import cc.uncarbon.module.sys.MybatisPlusTestSupport;
import cc.uncarbon.module.sys.dal.entity.SysOperateLogEntity;
import cc.uncarbon.module.sys.dal.mapper.SysOperateLogMapper;
import cc.uncarbon.module.sys.enums.LogResultStatusEnum;
import cc.uncarbon.module.sys.model.query.AdminSysOperateLogListQuery;
import cc.uncarbon.module.sys.model.request.SysOperateLogCreateRequest;
import cc.uncarbon.module.sys.model.valueobj.SysOperateLogDTO;
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
 * {@link SysOperateLogServiceImpl} 查询条件分支、UA 截断与 IP 归属地解析测试
 */
@ExtendWith(MockitoExtension.class)
class SysOperateLogServiceImplTest {

    private static final String CHROME_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    @Mock
    private SysOperateLogMapper sysOperateLogMapper;
    @Mock
    private IPLocationResolver primaryResolver;
    @Mock
    private IPLocationResolver fallbackResolver;
    private SysOperateLogServiceImpl service;

    @BeforeAll
    static void initTableInfoCache() {
        MybatisPlusTestSupport.initTableInfo();
    }

    @BeforeEach
    void setUp() {
        // 构造器含 List<IPLocationResolver>，@InjectMocks 无法按泛型注入，参照 SysDictServiceImplTest 手动构造
        service = new SysOperateLogServiceImpl(sysOperateLogMapper, List.of(primaryResolver, fallbackResolver),
                AuditNicknameFillerTestSupport.passThrough());
    }


    /*
    ----------------------------------------------------------------
                        后台管理-分页查询
    ----------------------------------------------------------------
     */

    @Test
    void adminListAppliesAllConditions() {
        Mockito.when(sysOperateLogMapper.selectPage(Mockito.any(), Mockito.any())).thenAnswer(inv -> {
            Page<SysOperateLogEntity> page = inv.getArgument(0);
            page.setRecords(List.of(entity()));
            page.setTotal(1);
            return page;
        });

        PageResult<SysOperateLogDTO> ret = service.adminList(new AdminSysOperateLogListQuery()
                .setPageParam(new PageParam(1, 10))
                .setBizType("sysUser")
                .setBehavior("create")
                .setBizNo("BIZ-1")
                .setUserId(1001L)
                .setResultStatus(LogResultStatusEnum.SUCCESS)
                .setBeginAt(Instant.parse("2026-01-01T00:00:00Z"))
                .setEndAt(Instant.parse("2026-02-01T00:00:00Z")));

        Assertions.assertEquals(1, ret.getTotal());
        Assertions.assertEquals(1, ret.getRecords().size());
        // 操作人ID
        Assertions.assertEquals(1001L, ret.getRecords().getFirst().getUserId());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysOperateLogEntity>> wrapperCaptor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        Mockito.verify(sysOperateLogMapper).selectPage(Mockito.any(), wrapperCaptor.capture());
        // 三个模糊 + 两个等值 + 一个时间区间条件同时生效
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        Assertions.assertTrue(sqlSegment.contains("biz_type LIKE"));
        Assertions.assertTrue(sqlSegment.contains("behavior LIKE"));
        Assertions.assertTrue(sqlSegment.contains("biz_no LIKE"));
        Assertions.assertTrue(sqlSegment.contains("user_id ="));
        Assertions.assertTrue(sqlSegment.contains("result_status ="));
        Assertions.assertTrue(sqlSegment.contains("created_at BETWEEN"));
    }

    @Test
    void adminListWithoutConditionsSkipsFilters() {
        Mockito.when(sysOperateLogMapper.selectPage(Mockito.any(), Mockito.any())).thenAnswer(inv -> {
            Page<SysOperateLogEntity> page = inv.getArgument(0);
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        });

        PageResult<SysOperateLogDTO> ret = service.adminList(new AdminSysOperateLogListQuery()
                .setPageParam(new PageParam(1, 10)));

        // 空结果走 convertList 空集合分支
        Assertions.assertEquals(0, ret.getTotal());
        Assertions.assertTrue(ret.getRecords().isEmpty());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysOperateLogEntity>> wrapperCaptor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        Mockito.verify(sysOperateLogMapper).selectPage(Mockito.any(), wrapperCaptor.capture());
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
    void createTruncatesOverlongUserAgentAndResolvesIpLocation() {
        Mockito.when(sysOperateLogMapper.insert(Mockito.any(SysOperateLogEntity.class))).thenAnswer(inv -> {
            inv.<SysOperateLogEntity>getArgument(0).setId(88L);
            return 1;
        });
        Mockito.when(primaryResolver.resolve("8.8.8.8")).thenReturn("中国|0|北京市|北京市|联通");

        String overlongUa = "M".repeat(SysOperateLogServiceImpl.USER_AGENT_MAX_LENGTH + 10);
        Long id = service.create(request(overlongUa, "8.8.8.8"));

        Assertions.assertEquals(88L, id);
        ArgumentCaptor<SysOperateLogEntity> captor = ArgumentCaptor.forClass(SysOperateLogEntity.class);
        Mockito.verify(sysOperateLogMapper).insert(captor.capture());
        SysOperateLogEntity saved = captor.getValue();
        // 超长度 UA 截断到上限
        Assertions.assertEquals(SysOperateLogServiceImpl.USER_AGENT_MAX_LENGTH, saved.getVisitorUserAgent().length());
        Assertions.assertEquals("中国|0|北京市|北京市|联通", saved.getVisitorIpLocation());
        Assertions.assertEquals("sysUser", saved.getBizType());
    }

    @Test
    void createFallsThroughResolversUntilHit() {
        Mockito.when(sysOperateLogMapper.insert(Mockito.any(SysOperateLogEntity.class))).thenReturn(1);
        // 首选解析器返回 null，继续尝试兜底解析器
        Mockito.when(primaryResolver.resolve("1.2.3.4")).thenReturn(null);
        Mockito.when(fallbackResolver.resolve("1.2.3.4")).thenReturn("内网");

        service.create(request("UA", "1.2.3.4"));

        ArgumentCaptor<SysOperateLogEntity> captor = ArgumentCaptor.forClass(SysOperateLogEntity.class);
        Mockito.verify(sysOperateLogMapper).insert(captor.capture());
        Assertions.assertEquals("内网", captor.getValue().getVisitorIpLocation());
    }

    @Test
    void createKeepsNullIpLocationWhenNoResolverHit() {
        Mockito.when(sysOperateLogMapper.insert(Mockito.any(SysOperateLogEntity.class))).thenReturn(1);
        Mockito.when(primaryResolver.resolve("1.2.3.4")).thenReturn(null);
        Mockito.when(fallbackResolver.resolve("1.2.3.4")).thenReturn(null);

        service.create(request("UA", "1.2.3.4"));

        ArgumentCaptor<SysOperateLogEntity> captor = ArgumentCaptor.forClass(SysOperateLogEntity.class);
        Mockito.verify(sysOperateLogMapper).insert(captor.capture());
        // 所有解析器均未命中 → 归属地保持 null
        Assertions.assertNull(captor.getValue().getVisitorIpLocation());
    }

    @Test
    void createSkipsResolversWhenVisitorIpNull() {
        Mockito.when(sysOperateLogMapper.insert(Mockito.any(SysOperateLogEntity.class))).thenReturn(1);

        service.create(request("UA", null));

        // visitorIp 为 null → 不触发任何解析器
        Mockito.verifyNoInteractions(primaryResolver, fallbackResolver);
        ArgumentCaptor<SysOperateLogEntity> captor = ArgumentCaptor.forClass(SysOperateLogEntity.class);
        Mockito.verify(sysOperateLogMapper).insert(captor.capture());
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
        Mockito.verifyNoInteractions(sysOperateLogMapper);
    }

    @Test
    void getByIdMissingReturnsNull() {
        Mockito.when(sysOperateLogMapper.selectById(9L)).thenReturn(null);

        Assertions.assertNull(service.getById(9L));
    }

    @Test
    void getByIdMapsOperatorFields() {
        Mockito.when(sysOperateLogMapper.selectById(9L)).thenReturn(entity());

        SysOperateLogDTO ret = service.getById(9L);

        Assertions.assertEquals(9L, ret.getId());
        // 操作人ID
        Assertions.assertEquals(1001L, ret.getUserId());
        Assertions.assertEquals("sysUser", ret.getBizType());
        Assertions.assertEquals(CHROME_UA, ret.getVisitorUserAgent());
    }

    @Test
    void getNonnullByIdThrowsWhenMissing() {
        Mockito.when(sysOperateLogMapper.selectById(9L)).thenReturn(null);

        Assertions.assertThrows(NoRecordException.class, () -> service.getNonnullById(9L));
    }

    @Test
    void getNonnullByIdReturnsDtoWhenExists() {
        Mockito.when(sysOperateLogMapper.selectById(9L)).thenReturn(entity());

        Assertions.assertEquals(9L, service.getNonnullById(9L).getId());
    }


    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    private SysOperateLogEntity entity() {
        SysOperateLogEntity entity = new SysOperateLogEntity()
                .setId(9L)
                .setBizType("sysUser")
                .setBehavior("create")
                .setBizNo("BIZ-1")
                .setVisitorIp("8.8.8.8")
                .setVisitorUserAgent(CHROME_UA)
                .setResultStatus(LogResultStatusEnum.SUCCESS)
                .setUserId(1001L);
        return entity;
    }

    private SysOperateLogCreateRequest request(String ua, String visitorIp) {
        return new SysOperateLogCreateRequest()
                .setBizType("sysUser")
                .setBehavior("create")
                .setBizNo("BIZ-1")
                .setOperation("新增用户")
                .setVisitorIp(visitorIp)
                .setVisitorUserAgent(ua)
                .setResultStatus(LogResultStatusEnum.SUCCESS);
    }
}
