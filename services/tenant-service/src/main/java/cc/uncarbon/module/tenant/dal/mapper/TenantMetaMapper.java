package cc.uncarbon.module.tenant.dal.mapper;

import cc.uncarbon.module.tenant.dal.entity.TenantMetaEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户主数据
 */
@Mapper
public interface TenantMetaMapper extends BaseMapper<TenantMetaEntity> {
	
}
