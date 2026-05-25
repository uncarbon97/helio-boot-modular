package cc.uncarbon.module.sys.dal.mapper;

import cc.uncarbon.module.sys.dal.entity.SysUserDeptRelationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 后台用户-部门关联
 */
@Mapper
public interface SysUserDeptRelationMapper extends BaseMapper<SysUserDeptRelationEntity> {
	
}
