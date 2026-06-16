package cc.uncarbon.module.sys.dal.mapper;

import cc.uncarbon.framework.helium.db.constant.SQLSegment;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

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

}
