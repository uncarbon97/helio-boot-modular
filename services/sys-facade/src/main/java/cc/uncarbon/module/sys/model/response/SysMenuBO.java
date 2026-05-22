package cc.uncarbon.module.sys.model.response;

import cc.uncarbon.framework.core.enums.EnabledStatusEnum;
import cc.uncarbon.module.sys.enums.SysMenuTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 系统菜单BO
 */
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SysMenuBO implements Serializable {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "创建时刻")
    private LocalDateTime createdAt;

    @Schema(description = "更新时刻")
    private LocalDateTime updatedAt;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "上级菜单ID(根菜单设置为0)")
    private Long parentId;

    @Schema(description = "菜单类型")
    private SysMenuTypeEnum menuType;

    @Schema(description = "菜单权限标识")
    private String permission;

    @Schema(description = "状态")
    private EnabledStatusEnum status;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "前端组件名称")
    private String component;

    @Schema(description = "外链地址")
    private String externalLink;

    @Schema(description = "【用于Vben Admin】路由地址", hidden = true)
    private String path;

    @Schema(description = "【用于Vben Admin】菜单名(全局唯一, 不能重复)", hidden = true)
    private String menuName;

    @Schema(description = "【用于Vben Admin】菜单详情", hidden = true)
    private VbenAdminMenuMetaVO meta;

}
