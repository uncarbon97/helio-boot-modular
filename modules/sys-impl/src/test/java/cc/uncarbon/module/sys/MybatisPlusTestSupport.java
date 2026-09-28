package cc.uncarbon.module.sys;

import cc.uncarbon.module.sys.dal.entity.*;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;

/**
 * 纯 Mockito 单测环境未启动 MyBatis-Plus，LambdaQueryWrapper 解析列名依赖实体元数据缓存，
 * 未初始化会报 can not find lambda cache for this entity。
 * 缓存为 JVM 级静态共享，各测试类在 @BeforeAll 中调用本方法，避免依赖执行顺序。
 */
public final class MybatisPlusTestSupport {

    private MybatisPlusTestSupport() {
    }

    public static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        Class<?>[] entityClasses = {
                SysDeptEntity.class,
                SysDictCategoryEntity.class,
                SysDictItemEntity.class,
                SysLoginLogEntity.class,
                SysMenuEntity.class,
                SysOperateLogEntity.class,
                SysRoleEntity.class,
                SysRoleMenuRelationEntity.class,
                SysUserDeptRelationEntity.class,
                SysUserEntity.class,
                SysUserRoleRelationEntity.class,
                SysUserTenantRelationEntity.class,
        };
        for (Class<?> entityClass : entityClasses) {
            TableInfoHelper.initTableInfo(assistant, entityClass);
        }
    }
}
