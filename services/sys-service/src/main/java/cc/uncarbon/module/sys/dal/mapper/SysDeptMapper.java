package cc.uncarbon.module.sys.dal.mapper;

import cc.uncarbon.module.sys.dal.entity.SysDeptEntity;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 部门
 */
@Mapper
public interface SysDeptMapper extends BaseMapper<SysDeptEntity> {

    /**
     * 列举已排序好的所有部门列表
     */
    default List<SysDeptEntity> selectSortedList() {
        return selectList(new LambdaQueryWrapper<SysDeptEntity>().orderByAsc(SysDeptEntity::getSort));
    }

}
