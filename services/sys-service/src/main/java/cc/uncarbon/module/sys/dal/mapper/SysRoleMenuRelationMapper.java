package cc.uncarbon.module.sys.dal.mapper;

import cc.uncarbon.module.sys.dal.entity.SysRoleMenuRelationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统角色-可见菜单关联
 */
@Mapper
public interface SysRoleMenuRelationMapper extends BaseMapper<SysRoleMenuRelationEntity> {
	
}
