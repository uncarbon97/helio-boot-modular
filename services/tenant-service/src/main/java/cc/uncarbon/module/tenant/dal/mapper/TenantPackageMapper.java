package cc.uncarbon.module.tenant.dal.mapper;

import cc.uncarbon.module.tenant.dal.entity.TenantPackageEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户套餐-菜单关联关系
 */
@Mapper
public interface TenantPackageMapper extends BaseMapper<TenantPackageEntity> {
	
}
