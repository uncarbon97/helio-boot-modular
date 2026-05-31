package cc.uncarbon.module.tenant.model.valueobj;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * 系统租户-需强制登出用户 BO
 * 同一时间大量登出，会操作大量Redis键，可能存在缓存雪崩的风险
 */
@Getter
public class TenantMetaKickOutUsersBO {

    @Schema(description = "后台用户IDs")
    private final List<Long> sysUserIds;

    public TenantMetaKickOutUsersBO() {
        this.sysUserIds = Collections.emptyList();
    }

    public TenantMetaKickOutUsersBO(List<Long> sysUserIds) {
        this.sysUserIds = sysUserIds;
    }
}
