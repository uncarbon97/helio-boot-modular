package cc.uncarbon.module.sys.dal.mapper;

import cc.uncarbon.module.commons.constant.SQLSegment;
import cc.uncarbon.module.sys.dal.entity.SysDictCategoryEntity;
import cc.uncarbon.module.sys.enums.DictStatusEnum;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;


/**
 * 字典分类
 */
@Mapper
public interface SysDictCategoryMapper extends BaseMapper<SysDictCategoryEntity> {

    default SysDictCategoryEntity selectByCodeAndStatus(@NonNull String code, @Nullable Collection<DictStatusEnum> statuses) {
        return selectOne(new LambdaQueryWrapper<SysDictCategoryEntity>()
                .eq(SysDictCategoryEntity::getCode, code)
                // 状态集合
                .in(CollUtil.isNotEmpty(statuses), SysDictCategoryEntity::getStatus, statuses)
                .last(SQLSegment.LIMIT_1)
        );
    }

}
