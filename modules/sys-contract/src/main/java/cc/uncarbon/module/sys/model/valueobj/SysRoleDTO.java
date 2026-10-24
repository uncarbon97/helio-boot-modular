package cc.uncarbon.module.sys.model.valueobj;

import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.commons.model.valueobj.AuditUserDisplayAware;
import cc.uncarbon.module.sys.enums.SysRoleFlagEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.List;


/**
 * 系统角色
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class SysRoleDTO implements Serializable, AuditUserDisplayAware {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "创建时刻")
    private Instant createdAt;

    @Schema(description = "创建者")
    private String createdBy;

    @Schema(description = "更新时刻")
    private Instant updatedAt;

    @Schema(description = "更新者")
    private String updatedBy;

    @Schema(description = "角色编码")
    private String code;

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "状态")
    private EnabledStatusEnum status;

    @Schema(description = "角色特殊标记")
    private List<SysRoleFlagEnum> flags;

    @Schema(description = "可见菜单Ids")
    private Collection<Long> menuIds;

}
