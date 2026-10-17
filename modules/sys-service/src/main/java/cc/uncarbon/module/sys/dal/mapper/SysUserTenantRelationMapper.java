package cc.uncarbon.module.sys.dal.mapper;

import cc.uncarbon.module.sys.dal.entity.SysUserTenantRelationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户-租户关联关系
 */
@Mapper
public interface SysUserTenantRelationMapper extends BaseMapper<SysUserTenantRelationEntity> {

}
