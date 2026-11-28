package cc.uncarbon.module.sys.dal.mapper;

import cc.uncarbon.module.sys.MybatisPlusTestSupport;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * {@link SysUserMapper#clearPiiContact} 专用清空默认方法测试：
 * 敏感字段与派生字段（masked/盲索引）一并置 NULL，且至少清空其一
 */
class SysUserMapperClearPiiContactTest {

    @BeforeAll
    static void initTableInfoCache() {
        MybatisPlusTestSupport.initTableInfo();
    }


    @Test
    void clearNothingRejected() {
        SysUserMapper mapper = Mockito.mock(SysUserMapper.class, Mockito.CALLS_REAL_METHODS);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> mapper.clearPiiContact(5L, false, false));
        Mockito.verify(mapper, Mockito.never()).update(Mockito.any(), Mockito.any());
    }

    @Test
    void clearPhoneOnlyNullsPhoneDerivedColumns() {
        SysUserMapper mapper = Mockito.mock(SysUserMapper.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(mapper.update(Mockito.any(SysUserEntity.class), Mockito.any())).thenReturn(1);

        mapper.clearPiiContact(5L, true, false);

        String sqlSet = captureSqlSet(mapper);
        Assertions.assertEquals(4, sqlSet.split(",").length);
        Assertions.assertTrue(sqlSet.contains("phone_no="));
        Assertions.assertTrue(sqlSet.contains("phone_no_masked="));
        Assertions.assertTrue(sqlSet.contains("phone_no_full_blind_index="));
        Assertions.assertTrue(sqlSet.contains("phone_no_tail4_blind_index="));
    }

    @Test
    void clearBothNullsAllContactColumns() {
        SysUserMapper mapper = Mockito.mock(SysUserMapper.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(mapper.update(Mockito.any(SysUserEntity.class), Mockito.any())).thenReturn(1);

        mapper.clearPiiContact(5L, true, true);

        String sqlSet = captureSqlSet(mapper);
        Assertions.assertEquals(7, sqlSet.split(",").length);
        Assertions.assertTrue(sqlSet.contains("email="));
        Assertions.assertTrue(sqlSet.contains("email_masked="));
        Assertions.assertTrue(sqlSet.contains("email_full_blind_index="));
    }


    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    @SuppressWarnings("unchecked")
    private static String captureSqlSet(SysUserMapper mapper) {
        ArgumentCaptor<LambdaUpdateWrapper<SysUserEntity>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        Mockito.verify(mapper).update(Mockito.any(SysUserEntity.class), wrapperCaptor.capture());
        // update(entity, wrapper) 的 WHERE 只来自 wrapper 条件
        Assertions.assertTrue(wrapperCaptor.getValue().getSqlSegment().contains("id"));

        return wrapperCaptor.getValue().getSqlSet();
    }
}
