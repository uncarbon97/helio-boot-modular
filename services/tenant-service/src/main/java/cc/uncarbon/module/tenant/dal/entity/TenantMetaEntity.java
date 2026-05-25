package cc.uncarbon.module.tenant.dal.entity;

import cc.uncarbon.framework.helium.db.entity.AbstractGenericEntity;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
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
 * 租户主数据
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName(value = "tenant_meta")
public class TenantMetaEntity extends AbstractGenericEntity {

    @Serial
    private static final long serialVersionUID = 1L;


    @Schema(description = "主键ID")
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @Schema(description = "租户编码")
    @TableField(value = "code")
    private String code;

    @Schema(description = "租户名称")
    @TableField(value = "name")
    private String name;

    @Schema(description = "状态")
    @TableField(value = "status")
    private EnabledStatusEnum status;

    @Schema(description = "租户管理员用户ID")
    @TableField(value = "admin_user_id")
    private Long adminUserId;

    @Schema(description = "所属租户套餐ID")
    @TableField(value = "package_id")
    private Long packageId;

}
