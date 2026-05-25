package cc.uncarbon.module.sys.model.request;

import cc.uncarbon.module.sys.enums.SysMenuTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 系统管理-新增/编辑系统菜单
 */
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminInsertOrUpdateSysMenuDTO implements Serializable {

    @Schema(description = "主键ID", hidden = true, title = "仅更新时使用")
    private Long id;

    @Schema(description = "菜单名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 50, message = "【菜单名称】最长50位")
    @NotBlank(message = "菜单名称必填")
    private String name;

    @Schema(description = "上级菜单ID(根菜单设置为0)")
    private Long parentId;

    @Schema(description = "菜单类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "菜单类型必填")
    private SysMenuTypeEnum menuType;

    @Schema(description = "前端组件名称")
    @Size(max = 50, message = "【前端组件名称】最长50位")
    private String component;

    @Schema(description = "菜单权限标识")
    @Size(max = 255, message = "【权限标识】最长255位")
    private String permission;

    @Schema(description = "图标")
    @Size(max = 255, message = "【图标】最长255位")
    private String icon;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态")
    private EnabledStatusEnum status;

    @Schema(description = "外链地址")
    @Size(max = 255, message = "【外链地址】最长255位")
    private String externalLink;

}
