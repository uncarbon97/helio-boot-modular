package cc.uncarbon.module.sys.model.query;

import cc.uncarbon.framework.helium.base.page.PageParam;
import cc.uncarbon.module.sys.constant.SysConstant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link AdminSysUserListQuery} 单元测试
 */
class AdminSysUserListQueryTest {

    /**
     * 未选择部门（null）时无需按部门筛选
     */
    @Test
    void nullSelectedDeptIdDoesNotNeedFilter() {
        Assertions.assertFalse(new AdminSysUserListQuery().needFilterBySelectedDeptId());
        Assertions.assertFalse(new AdminSysUserListQuery(null, "13800138000", null)
                .needFilterBySelectedDeptId());
    }

    /**
     * 选择根父级 ID（0）时无需按部门筛选
     */
    @Test
    void rootParentIdDoesNotNeedFilter() {
        AdminSysUserListQuery query = new AdminSysUserListQuery().setSelectedDeptId(SysConstant.ROOT_PARENT_ID);

        Assertions.assertFalse(query.needFilterBySelectedDeptId());
    }

    /**
     * 选择负数部门 ID 时无需按部门筛选
     */
    @Test
    void negativeDeptIdDoesNotNeedFilter() {
        Assertions.assertFalse(new AdminSysUserListQuery().setSelectedDeptId(-1L)
                .needFilterBySelectedDeptId());
    }

    /**
     * 选择正数部门 ID 时需要按部门筛选
     */
    @Test
    void positiveDeptIdNeedsFilter() {
        Assertions.assertTrue(new AdminSysUserListQuery().setSelectedDeptId(1L)
                .needFilterBySelectedDeptId());
    }

    /**
     * 无参构造器所有字段应为 null
     */
    @Test
    void noArgsConstructorDefaultsAllNull() {
        AdminSysUserListQuery query = new AdminSysUserListQuery();

        Assertions.assertNull(query.getPageParam());
        Assertions.assertNull(query.getPhoneNo());
        Assertions.assertNull(query.getSelectedDeptId());
    }

    /**
     * 链式 setter 应覆盖全部分页查询字段
     */
    @Test
    void allArgsConstructorAndChainSetters() {
        PageParam pageParam = new PageParam();
        AdminSysUserListQuery query = new AdminSysUserListQuery()
                .setPageParam(pageParam)
                .setPhoneNo("13800138000")
                .setSelectedDeptId(100L);

        Assertions.assertSame(pageParam, query.getPageParam());
        Assertions.assertEquals("13800138000", query.getPhoneNo());
        Assertions.assertEquals(100L, query.getSelectedDeptId());
    }

    /**
     * Lombok equals/hashCode 应按全字段比较
     */
    @Test
    void lombokEqualsAndHashCode() {
        AdminSysUserListQuery left = new AdminSysUserListQuery(null, "13800138000", 100L);
        AdminSysUserListQuery right = new AdminSysUserListQuery().setPhoneNo("13800138000").setSelectedDeptId(100L);

        Assertions.assertEquals(left, right);
        Assertions.assertEquals(left.hashCode(), right.hashCode());
        Assertions.assertNotEquals(left, new AdminSysUserListQuery().setSelectedDeptId(101L));
    }
}
