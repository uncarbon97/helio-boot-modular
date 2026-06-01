package cc.uncarbon.module.tenant.dal.entity;

import cc.uncarbon.framework.helium.db.entity.AbstractRelationEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;


/**
 * 租户套餐-菜单关联关系
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName(value = "tenant_package_menu_relation")
public class TenantPackageMenuRelationEntity extends AbstractRelationEntity {

    @Serial
    private static final long serialVersionUID = 1L;


    @Schema(description = "主键ID")
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @Schema(description = "租户套餐ID")
    @TableField(value = "package_id")
    private Long packageId;

    @Schema(description = "菜单ID")
    @TableField(value = "menu_id")
    private Long menuId;

    public static TenantPackageMenuRelationEntity of(Long packageId, Long menuId) {
        return new TenantPackageMenuRelationEntity()
                .setPackageId(packageId)
                .setMenuId(menuId);
    }

}
