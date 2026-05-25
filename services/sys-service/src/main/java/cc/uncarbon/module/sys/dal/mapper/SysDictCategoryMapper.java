package cc.uncarbon.module.sys.dal.mapper;

import cc.uncarbon.framework.helium.db.constant.SQLSegment;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.sys.dal.entity.SysDictCategoryEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;


import java.util.Objects;


/**
 * 字典分类
 */
@Mapper
public interface SysDictCategoryMapper extends BaseMapper<SysDictCategoryEntity> {

    default SysDictCategoryEntity selectByCodeAndStatus(@NonNull String code, @Nullable EnabledStatusEnum status) {
        return selectOne(
                new QueryWrapper<SysDictCategoryEntity>()
                        .lambda()
                        .eq(SysDictCategoryEntity::getCode, code)
                        .eq(Objects.nonNull(status), SysDictCategoryEntity::getStatus, status)
                        .last(SQLSegment.LIMIT_1)
        );
    }

}
