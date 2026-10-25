package cc.uncarbon.module.sys.dal.entity;

import cc.uncarbon.framework.helium.db.entity.AbstractTenantGenericEntity;
import cc.uncarbon.framework.helium.db.enums.GenderEnum;
import cc.uncarbon.framework.helium.db.enums.YesOrNoEnum;
import cc.uncarbon.framework.helium.db.fieldcrypto.annotation.BlindIndexOf;
import cc.uncarbon.framework.helium.db.fieldcrypto.annotation.MaskedOf;
import cc.uncarbon.framework.helium.db.fieldcrypto.annotation.TableFieldCrypto;
import cc.uncarbon.framework.helium.db.fieldcrypto.blindindex.BlindIndexValueExtractor;
import cc.uncarbon.framework.helium.db.fieldcrypto.blindindex.BlindIndexValueExtractors;
import cc.uncarbon.module.commons.model.valueobj.AuditingUserSource;
import cc.uncarbon.module.sys.constant.SysUserCipherCols;
import cc.uncarbon.module.sys.dal.fieldcrypto.EmailEncryptedTypeHandler;
import cc.uncarbon.module.sys.dal.fieldcrypto.EmailFieldValueMasker;
import cc.uncarbon.module.sys.dal.fieldcrypto.PhoneEncryptedTypeHandler;
import cc.uncarbon.module.sys.dal.fieldcrypto.PhoneFieldValueMasker;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import cc.uncarbon.module.sys.util.PiiContractNormalizer;
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
@TableName(value = "sys_user", autoResultMap = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
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

    @Schema(description = "要求用户下次登录时修改密码")
    @TableField(value = "pwd_must_change")
    private YesOrNoEnum pwdMustChange;

    @Schema(description = "昵称")
    @TableField(value = "nickname")
    private String nickname;

    @Schema(description = "性别")
    @TableField(value = "gender")
    private GenderEnum gender;

    @Schema(description = "状态")
    @TableField(value = "status")
    private SysUserStatusEnum status;

    @Schema(description = "最后登录时刻")
    @TableField(value = "last_login_at")
    private Instant lastLoginAt;

    @Schema(description = "头像URL")
    @TableField(value = "avatar_url")
    private String avatarUrl;

    @Schema(description = "手机号（在内存中明文）")
    @TableField(value = "phone_no", typeHandler = PhoneEncryptedTypeHandler.class)
    @ToString.Exclude
    @TableFieldCrypto(columnPath = SysUserCipherCols.PHONE_COLUMN_PATH)
    private String phoneNo;

    @Schema(description = "手机号脱敏冗余")
    @TableField(value = "phone_no_masked")
    @MaskedOf(value = "phoneNo", masker = PhoneFieldValueMasker.class)
    private String phoneNoMasked;

    @Schema(description = "手机号全串盲索引")
    @TableField(value = "phone_no_full_blind_index")
    @BlindIndexOf(value = "phoneNo", purpose = SysUserCipherCols.PHONE_FULL_PURPOSE,
            extractor = BlindIndexValueExtractors.CleanTrim.class)
    private String phoneNoFullBlindIndex;

    @Schema(description = "手机号后4位盲索引")
    @TableField(value = "phone_no_tail4_blind_index")
    @BlindIndexOf(value = "phoneNo", purpose = SysUserCipherCols.PHONE_TAIL4_PURPOSE, extractor = Tail4Digits.class)
    private String phoneNoTail4BlindIndex;

    @Schema(description = "邮箱（在内存中明文）")
    @TableField(value = "email", typeHandler = EmailEncryptedTypeHandler.class)
    @ToString.Exclude
    @TableFieldCrypto(columnPath = SysUserCipherCols.EMAIL_COLUMN_PATH)
    private String email;

    @Schema(description = "邮箱脱敏冗余")
    @TableField(value = "email_masked")
    @MaskedOf(value = "email", masker = EmailFieldValueMasker.class)
    private String emailMasked;

    @Schema(description = "邮箱全串盲索引")
    @TableField(value = "email_full_blind_index")
    @BlindIndexOf(value = "email", purpose = SysUserCipherCols.EMAIL_FULL_PURPOSE,
            extractor = BlindIndexValueExtractors.LowerTrim.class)
    private String emailFullBlindIndex;


    /**
     * 归一化手机号后赋值
     */
    public SysUserEntity setCleanPhoneNo(String raw) {
        return setPhoneNo(PiiContractNormalizer.normalizePhone(raw));
    }

    /**
     * 归一化邮箱后
     */
    public SysUserEntity setCleanEmail(String raw) {
        return setEmail(PiiContractNormalizer.normalizeEmail(raw));
    }


    public static class Tail4Digits implements BlindIndexValueExtractor {
        public Tail4Digits() {
        }

        @Override
        public String extract(String rawValue) {
            // 与「手机号全串盲索引」的处理过程一致，先走 CleanTrim
            final String cleaned = PiiContractNormalizer.normalizePhone(rawValue);
            return BlindIndexValueExtractors.TailDigits.of(4).extract(cleaned);
        }
    }
}
