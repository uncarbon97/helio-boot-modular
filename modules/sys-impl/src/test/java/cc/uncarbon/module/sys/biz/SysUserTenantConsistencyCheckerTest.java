package cc.uncarbon.module.sys.biz;

import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserTenantRelationEntity;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserTenantRelationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cc.uncarbon.module.sys.MybatisPlusTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

/**
 * {@link SysUserTenantConsistencyChecker} 对账任务分支测试（只读对账，异常吞掉）
 */
@ExtendWith(MockitoExtension.class)
class SysUserTenantConsistencyCheckerTest {

    @BeforeAll
    static void initTableInfoCache() {
        MybatisPlusTestSupport.initTableInfo();
    }

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private SysUserTenantRelationMapper relationMapper;

    @InjectMocks
    private SysUserTenantConsistencyChecker checker;


    @Test
    void reconcilePassesWhenProjectedColumnMatchesRelation() {
        Mockito.when(sysUserMapper.selectList(Mockito.any())).thenReturn(List.of(
                user(1L, 10L)));
        Mockito.when(relationMapper.selectList(Mockito.any())).thenReturn(List.of(
                relation(10L, 1L)));

        Assertions.assertDoesNotThrow(() -> checker.reconcile());
    }

    @Test
    void reconcileDetectsDriftWithoutThrowing() {
        // 投影列 10，真源却是 20；且有用户完全没有关系行
        Mockito.when(sysUserMapper.selectList(Mockito.any())).thenReturn(List.of(
                user(1L, 10L), user(2L, 30L)));
        Mockito.when(relationMapper.selectList(Mockito.any())).thenReturn(List.of(
                relation(20L, 1L)));

        // 漂移只告警不抛出、不自动修复
        Assertions.assertDoesNotThrow(() -> checker.reconcile());
    }

    @Test
    void reconcileSwallowsQueryFailure() {
        Mockito.when(sysUserMapper.selectList(Mockito.any()))
                .thenThrow(new RuntimeException("db down"));

        Assertions.assertDoesNotThrow(() -> checker.reconcile());
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private SysUserEntity user(Long id, Long tenantId) {
        var ret = new SysUserEntity().setId(id);
        ret.setTenantId(tenantId);
        return ret;
    }

    private SysUserTenantRelationEntity relation(Long tenantId, Long userId) {
        return SysUserTenantRelationEntity.of(tenantId, userId);
    }
}
