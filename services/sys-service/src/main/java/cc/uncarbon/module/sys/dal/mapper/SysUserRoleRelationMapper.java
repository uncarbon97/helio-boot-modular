package cc.uncarbon.module.sys.dal.mapper;

import cc.uncarbon.module.sys.dal.entity.SysUserRoleRelationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户-角色关联关系
 */
@Mapper
public interface SysUserRoleRelationMapper extends BaseMapper<SysUserRoleRelationEntity> {
	
}
