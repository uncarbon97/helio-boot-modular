package cc.uncarbon.module.sys.dal.mapper;

import cc.uncarbon.framework.helium.db.enums.YesOrNoEnum;
import cc.uncarbon.module.commons.constant.SQLSegment;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.util.Collection;

/**
 * 系统用户
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUserEntity> {

    /**
     * 分页查询未分配部门的用户
     */
    Page<SysUserEntity> pageNoDeptUser(
            Page<SysUserEntity> page,
            @Param("phoneNo") String phoneNo,
            @Param("invisibleUserIds") Collection<Long> invisibleUserIds
    );

    default SysUserEntity getByPin(String pin) {
        return selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getPin, pin)
                .last(SQLSegment.LIMIT_1)
        );
    }

    /**
     * 按 (账号, 租户ID) 精确定位用户
     * 租户优先模式下同一 pin 可存在于多个租户，需租户条件消歧
     */
    default SysUserEntity getByPinAndTenantId(String pin, Long tenantId) {
        if (pin == null || tenantId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getPin, pin)
                .eq(SysUserEntity::getTenantId, tenantId)
                .last(SQLSegment.LIMIT_1)
        );
    }

    /**
     * 只取用户归属租户ID（sys_user.tenant_id 单值）
     */
    default Long getUserTenantId(Long id) {
        var entity = selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .select(SysUserEntity::getTenantId)
                .eq(SysUserEntity::getId, id)
                .last(SQLSegment.LIMIT_1)
        );
        return entity == null ? null : entity.getTenantId();
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
}
