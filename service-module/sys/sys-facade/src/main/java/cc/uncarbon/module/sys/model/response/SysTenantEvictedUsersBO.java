package cc.uncarbon.module.sys.model.response;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * 系统租户-被强制踢出用户 BO
 */
@Getter
public class SysTenantEvictedUsersBO {

    @ApiModelProperty(value = "后台用户IDs")
    private final List<Long> sysUserIds;

    public SysTenantEvictedUsersBO() {
        this.sysUserIds = Collections.emptyList();
    }

    public SysTenantEvictedUsersBO(List<Long> sysUserIds) {
        this.sysUserIds = sysUserIds;
    }
}
