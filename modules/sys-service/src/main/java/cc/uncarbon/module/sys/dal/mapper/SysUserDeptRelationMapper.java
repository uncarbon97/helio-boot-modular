package cc.uncarbon.module.sys.dal.mapper;

import cc.uncarbon.module.sys.dal.entity.SysUserDeptRelationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户-部门关联关系
 */
@Mapper
public interface SysUserDeptRelationMapper extends BaseMapper<SysUserDeptRelationEntity> {
	
}
