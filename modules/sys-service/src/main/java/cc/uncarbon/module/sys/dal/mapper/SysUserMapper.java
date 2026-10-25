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
 *
 * <p>派生列（masked/盲索引）未声明 fill，由框架 FieldCryptoDerivedColumnInnerInterceptor 在 getBoundSql 前
 * 按实体是否携带明文来源回填：携带来源才重算进 SET，局部更新不携带来源则派生列不参与 SET、不误清存量。
 * wrapper 更新传空实体，仅借 fill 补盖 updated_at/updated_by</p>
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
        updateById(new SysUserEntity().setId(userId).setLastLoginAt(lastLoginAt));
    }

    /**
     * 更新头像URL
     */
    default void updateAvatarUrl(long userId, String avatarUrl) {
        updateById(new SysUserEntity().setId(userId).setAvatarUrl(avatarUrl));
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
        var uw = new LambdaUpdateWrapper<SysUserEntity>()
                .eq(SysUserEntity::getId, userId);
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
        update(new SysUserEntity(), uw);
    }

    /**
     * 仅更新用户记忆租户
     */
    default void updateTenantId(long userId, Long tenantId) {
        SysUserEntity entity = new SysUserEntity().setId(userId);
        entity.setTenantId(tenantId);
        updateById(entity);
    }
}
