package cc.uncarbon.module.sys.model.request;

import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.sys.enums.MenuTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 系统菜单-新增/修改
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminSysMenuUpsertRequest implements Serializable {

    @Schema(description = "主键ID", title = "仅修改时使用")
    private Long id;

    @Schema(description = "菜单名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 100, message = "菜单名称最长{max}位")
    @NotBlank(message = "菜单名称必填")
    private String name;

    @Schema(description = "上级菜单ID(根菜单设置为0)")
    private Long parentId;

    @Schema(description = "菜单类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "菜单类型必填")
    private MenuTypeEnum menuType;

    @Schema(description = "前端组件名称")
    @Size(max = 255, message = "前端组件名称最长{max}位")
    private String component;

    @Schema(description = "菜单权限标识")
    @Size(max = 255, message = "权限标识最长{max}位")
    private String permission;

    @Schema(description = "图标")
    @Size(max = 255, message = "图标最长{max}位")
    private String icon;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态")
    private EnabledStatusEnum status;

    @Schema(description = "外链地址")
    @Size(max = 255, message = "外链地址最长255位")
    private String externalLink;

}
