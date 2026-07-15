package cc.uncarbon.module.sys.dal.mapper;

import cc.uncarbon.module.commons.constant.SQLSegment;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * 系统用户
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUserEntity> {

    default SysUserEntity getByPin(String pin) {
        return selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getPin, pin)
                .last(SQLSegment.LIMIT_1)
        );
    }

    /**
     * 更新最后一次登录时刻
     */
    default void updateLastLoginAt(long userId, LocalDateTime lastLoginAt) {
        SysUserEntity update = new SysUserEntity();
        update.setLastLoginAt(lastLoginAt)
                .setId(userId);
        updateById(update);
    }

    default void updateEncryptedPwd(long userId, String encryptedPwd) {
        updateById(new SysUserEntity().setPwd(encryptedPwd).setId(userId));
    }

    /**
     * 分页查询未分配部门的用户
     */
    Page<SysUserEntity> pageNoDeptUser(
            Page<SysUserEntity> page,
            @Param("phoneNo") String phoneNo,
            @Param("invisibleUserIds") Collection<Long> invisibleUserIds
    );

}
