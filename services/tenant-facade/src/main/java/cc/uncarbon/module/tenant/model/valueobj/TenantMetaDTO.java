package cc.uncarbon.module.tenant.model.valueobj;

import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 租户主数据
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TenantMetaDTO implements Serializable {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "创建时刻")
    private LocalDateTime createdAt;

    @Schema(description = "更新时刻")
    private LocalDateTime updatedAt;

    @Schema(description = "租户编码")
    private String code;

    @Schema(description = "租户名称")
    private String name;

    @Schema(description = "状态")
    private EnabledStatusEnum status;

    @Schema(description = "租户管理员用户ID")
    private Long adminUserId;

    @Schema(description = "所属租户套餐ID")
    private Long packageId;

    @Schema(description = "租户管理员用户基本信息")
    private SysUserBaseInfoBO adminUser;

}
