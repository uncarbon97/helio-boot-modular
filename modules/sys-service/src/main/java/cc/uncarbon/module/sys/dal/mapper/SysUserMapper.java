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

    default void updateEncryptedPwd(long userId, String encryptedPwd, YesOrNoEnum pwdMustChange) {
        updateById(new SysUserEntity().setId(userId)
                .setPwd(encryptedPwd)
                .setPwdLastUpdatedAt(Instant.now())
                .setPwdMustChange(pwdMustChange)
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
     * 专用清空方法
     * <P>清空联系方式时，敏感字段 + 派生字段一并置 NULL
     */
    default void clearPiiContact(long userId, boolean clearPhone, boolean clearEmail) {
        if (!clearPhone && !clearEmail) {
            throw new IllegalArgumentException("至少需要清空手机号或邮箱之一");
        }
        var uw = new LambdaUpdateWrapper<SysUserEntity>();
        if (clearPhone) {
            uw.set(SysUserEntity::getPhoneNo, null)
                    .set(SysUserEntity::getPhoneNoMasked, null)
                    .set(SysUserEntity::getPhoneNoFullBlindIndex, null)
                    .set(SysUserEntity::getPhoneNoTail4BlindIndex, null);
        }
        if (clearEmail) {
            uw.set(SysUserEntity::getEmail, null)
                    .set(SysUserEntity::getEmailMasked, null)
                    .set(SysUserEntity::getEmailFullBlindIndex, null);
        }
        update(new SysUserEntity().setId(userId), uw);
    }
}
