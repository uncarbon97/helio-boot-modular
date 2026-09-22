package cc.uncarbon.module.sys.model.valueobj;

import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.sys.enums.MenuTypeEnum;
import cc.uncarbon.module.sys.enums.MenuVisibleScopeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.Instant;


/**
 * 系统菜单
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SysMenuDTO implements Serializable {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "创建时刻")
    private Instant createdAt;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "上级菜单ID(根菜单设置为0)")
    private Long parentId;

    @Schema(description = "菜单类型")
    private MenuTypeEnum menuType;

    @Schema(description = "路由地址(目录为可读slug, 页面为全路径)")
    private String path;

    @Schema(description = "授权标识")
    private String permission;

    @Schema(description = "状态")
    private EnabledStatusEnum status;

    @Schema(description = "可见范围")
    private MenuVisibleScopeEnum visibleScope;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序(数值越小越优先)")
    private Integer sort;

    @Schema(description = "前端组件名称")
    private String component;

    @Schema(description = "外链地址")
    private String externalLink;

}
