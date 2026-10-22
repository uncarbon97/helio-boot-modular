package cc.uncarbon.module.sys.dal.entity;

import cc.uncarbon.framework.helium.db.entity.AbstractTenantGenericEntity;
import cc.uncarbon.framework.helium.db.enums.GenderEnum;
import cc.uncarbon.framework.helium.db.enums.YesOrNoEnum;
import cc.uncarbon.framework.helium.fieldcrypto.annotation.BlindIndexOf;
import cc.uncarbon.framework.helium.fieldcrypto.annotation.BlindIndexValueExtractors;
import cc.uncarbon.framework.helium.fieldcrypto.annotation.MaskedOf;
import cc.uncarbon.framework.helium.fieldcrypto.annotation.UseFieldCrypto;
import cc.uncarbon.module.commons.model.valueobj.AuditingUserSource;
import cc.uncarbon.module.sys.constant.SysUserCipherCols;
import cc.uncarbon.module.sys.dal.masker.EmailFieldValueMasker;
import cc.uncarbon.module.sys.dal.masker.PhoneFieldValueMasker;
import cc.uncarbon.module.sys.dal.typehandler.SysUserEmailEncryptedTypeHandler;
import cc.uncarbon.module.sys.dal.typehandler.SysUserPhoneNoEncryptedTypeHandler;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.time.Instant;

/**
 * 系统用户
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName(value = "sys_user", autoResultMap = true)
public class SysUserEntity extends AbstractTenantGenericEntity implements AuditingUserSource {

    @Serial
    private static final long serialVersionUID = 1L;


    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "账号")
    @TableField(value = "pin")
    private String pin;

    @ToString.Exclude
    @Schema(description = "密码")
    @TableField(value = "pwd")
    private String pwd;

    @Schema(description = "密码最近一次更新时刻")
    @TableField(value = "pwd_last_updated_at")
    private Instant pwdLastUpdatedAt;

    @Schema(description = "昵称")
    @TableField(value = "nickname")
    private String nickname;

    @Schema(description = "状态")
    @TableField(value = "status")
    private SysUserStatusEnum status;

    @Schema(description = "最后登录时刻")
    @TableField(value = "last_login_at")
    private Instant lastLoginAt;

    @Schema(description = "性别")
    @TableField(value = "gender")
    private GenderEnum gender;

    @ToString.Exclude
    @Schema(description = "邮箱（内存中为解密后明文；日志脱敏见 @ToString.Exclude）")
    @TableField(value = "email_encrypted", typeHandler = SysUserEmailEncryptedTypeHandler.class)
    @UseFieldCrypto(aad = SysUserCipherCols.EMAIL_CIPHER_AAD)
    private String email;

    @Schema(description = "邮箱全串盲索引（自动填充，LowerTrim 归一化）")
    @TableField(value = "email_full_blind_index")
    @BlindIndexOf(valueField = "email", purpose = SysUserCipherCols.EMAIL_FULL_PURPOSE,
            extractor = BlindIndexValueExtractors.LowerTrim.class)
    private String emailFullBlindIndex;

    @Schema(description = "邮箱脱敏冗余列（自动填充，列表展示直读）")
    @TableField(value = "email_masked")
    @MaskedOf(valueField = "email", masker = EmailFieldValueMasker.class)
    private String emailMasked;

    @ToString.Exclude
    @Schema(description = "手机号（内存中为解密后明文；日志脱敏见 @ToString.Exclude）")
    @TableField(value = "phone_no_encrypted", typeHandler = SysUserPhoneNoEncryptedTypeHandler.class)
    @UseFieldCrypto(aad = SysUserCipherCols.PHONE_CIPHER_AAD)
    private String phoneNo;

    @Schema(description = "手机号全串盲索引（自动填充，CleanTrim 归一化；等值定位用）")
    @TableField(value = "phone_no_full_blind_index")
    @BlindIndexOf(valueField = "phoneNo", purpose = SysUserCipherCols.PHONE_FULL_PURPOSE,
            extractor = BlindIndexValueExtractors.CleanTrim.class)
    private String phoneNoFullBlindIndex;

    @Schema(description = "手机号后4位盲索引（自动填充；仅管理端尾号搜检索入口，允许碰撞）")
    @TableField(value = "phone_no_tail4_blind_index")
    @BlindIndexOf(valueField = "phoneNo", purpose = SysUserCipherCols.PHONE_TAIL_PURPOSE,
            extractor = BlindIndexValueExtractors.TailDigits.class)
    private String phoneNoTail4BlindIndex;

    @Schema(description = "手机号脱敏冗余列（自动填充，列表展示直读）")
    @TableField(value = "phone_no_masked")
    @MaskedOf(valueField = "phoneNo", masker = PhoneFieldValueMasker.class)
    private String phoneNoMasked;

    @Schema(description = "头像URL")
    @TableField(value = "avatar_url")
    private String avatarUrl;

    @Schema(description = "要求用户下次登录时修改密码")
    @TableField(value = "must_change_password")
    private YesOrNoEnum mustChangePassword;

}
