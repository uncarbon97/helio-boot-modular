package cc.uncarbon.module.sys.dal.mapper;

import cc.uncarbon.framework.helium.base.stream.StreamFunction;
import cc.uncarbon.framework.helium.db.enums.YesOrNoEnum;
import cc.uncarbon.module.commons.constant.SQLSegment;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

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

    default Map<Long, String> getPinMapByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) return Map.of();
        return selectList(new LambdaQueryWrapper<SysUserEntity>()
                .select(SysUserEntity::getId, SysUserEntity::getPin)
                .in(SysUserEntity::getId, ids)
        ).stream().collect(Collectors.toMap(SysUserEntity::getId, SysUserEntity::getPin, StreamFunction.keepExisting()));
    }

    /**
     * 更新最后一次登录时刻
     */
    default void updateLastLoginAt(long userId, Instant lastLoginAt) {
        SysUserEntity update = new SysUserEntity();
        update.setLastLoginAt(lastLoginAt)
                .setId(userId);
        updateById(update);
    }

    default void updateEncryptedPwd(long userId, String encryptedPwd, YesOrNoEnum mustChangePassword) {
        updateById(new SysUserEntity().setId(userId)
                .setPwd(encryptedPwd)
                .setPwdLastUpdatedAt(Instant.now())
                .setMustChangePassword(mustChangePassword)
        );
    }

    default void updateStatusBatch(@NonNull Collection<Long> ids, @NonNull SysUserStatusEnum status) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        update(new SysUserEntity(), new LambdaUpdateWrapper<SysUserEntity>()
                .in(SysUserEntity::getId, ids)
                .set(SysUserEntity::getStatus, status)
        );
    }

    /**
     * 专用清空方法：显式清空联系方式时，密文列 + 全部派生列（masked/盲索引）一并置 NULL，
     * 禁止半清留下「密文 NULL + 索引残留」脏数据（updateById 传空串清不掉派生列，即为此禁用）
     *
     * <p>guard 测试白名单：密文列 .set 的唯一合法路径（只封条件不封 set）</p>
     */
    default void clearPiiContact(long userId, boolean clearPhone, boolean clearEmail) {
        if (!clearPhone && !clearEmail) {
            throw new IllegalArgumentException("clearPiiContact 至少需要清空手机号或邮箱之一");
        }
        LambdaUpdateWrapper<SysUserEntity> wrapper = new LambdaUpdateWrapper<SysUserEntity>()
                .eq(SysUserEntity::getId, userId);
        if (clearPhone) {
            wrapper.set(SysUserEntity::getPhoneNo, null)
                    .set(SysUserEntity::getPhoneNoMasked, null)
                    .set(SysUserEntity::getPhoneNoBlindIndex, null)
                    .set(SysUserEntity::getPhoneNoTailBlindIndex, null);
        }
        if (clearEmail) {
            wrapper.set(SysUserEntity::getEmail, null)
                    .set(SysUserEntity::getEmailMasked, null)
                    .set(SysUserEntity::getEmailBlindIndex, null);
        }
        wrapper.set(SysUserEntity::getUpdatedAt, Instant.now());
        update(null, wrapper);
    }
}
