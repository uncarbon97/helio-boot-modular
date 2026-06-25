package cc.uncarbon.module.sys.dal.entity;

import cc.uncarbon.framework.helium.db.entity.AbstractTenantGenericEntity;
import cc.uncarbon.framework.helium.db.enums.GenderEnum;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
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
import java.time.LocalDateTime;


/**
 * 系统用户
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName(value = "sys_user")
public class SysUserEntity extends AbstractTenantGenericEntity {

    @Serial
    private static final long serialVersionUID = 1L;


    @Schema(description = "主键ID")
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @Schema(description = "账号")
    @TableField(value = "pin")
    private String pin;

    @Schema(description = "密码")
    @TableField(value = "pwd")
    private String pwd;

    @Schema(description = "密码加盐")
    @TableField(value = "pwd_salt")
    private String pwdSalt;

    @Schema(description = "昵称")
    @TableField(value = "nickname")
    private String nickname;

    @Schema(description = "状态")
    @TableField(value = "status")
    private SysUserStatusEnum status;

    @Schema(description = "最后登录时刻")
    @TableField(value = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Schema(description = "性别")
    @TableField(value = "gender")
    private GenderEnum gender;

    @Schema(description = "邮箱")
    @TableField(value = "email")
    private String email;

    @Schema(description = "手机号")
    @TableField(value = "phone_no")
    private String phoneNo;

    @Schema(description = "头像URL")
    @TableField(value = "avatar_url")
    private String avatarUrl;

}
