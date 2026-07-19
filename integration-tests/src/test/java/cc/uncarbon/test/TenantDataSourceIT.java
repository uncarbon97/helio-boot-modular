package cc.uncarbon.test;

import cc.uncarbon.framework.helium.base.page.PageParam;
import cc.uncarbon.module.sys.model.query.AdminSysDictCategoryListQuery;
import cc.uncarbon.module.sys.service.SysDictService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 数据源租户测试
 *
 * @author Uncarbon
 */
class TenantDataSourceIT extends BaseIntegrationTest {

    @Resource
    private SysDictService dictService;


    @Test
    void classifiedPageTest() {
        withContext(
                testUser(1L, "x"),
                testTenant(101L, "x", "x"),
                () -> {
                    var ret = dictService.adminListCategory(new AdminSysDictCategoryListQuery()
                            .setPageParam(new PageParam()));
                    Assertions.assertNotNull(ret);
                    Assertions.assertNotNull(ret.getRecords());
                });
    }
}
