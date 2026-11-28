package cc.uncarbon.module.sys.model.excel;

import cc.uncarbon.framework.helium.db.enums.GenderEnum;
import cc.uncarbon.framework.helium.excel.model.ExcelRow;
import cc.uncarbon.module.sys.model.request.AdminUpdateMyProfileRequest;
import cn.hutool.core.text.CharSequenceUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.fesod.sheet.annotation.ExcelProperty;

import java.io.Serializable;

/**
 * 系统用户
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class SysUserExcelRow implements ExcelRow, Serializable {

    public static final String MSG_PIN = "账号必填，账号最短5位，最长16位";

    public static final String MSG_NICKNAME_BLANK = "昵称必填";

    public static final String MSG_NICKNAME_LENGTH = "昵称最长20位";

    public static final String MSG_GENDER = "性别必填，仅支持 男/女/未知";

    public static final String MSG_PHONE = "手机号须为大陆11位手机号（留空表示不设置）";

    public static final String MSG_EMAIL = "邮箱格式有误（留空表示不设置）";

    public static final String TEMPLATE_TIPS = """
            1.%s；不可与现有用户重复，文件内不可重复
            2.%s；%s
            3.%s
            4.%s
            5.邮箱：仅支持ASCII字符，最长254位（留空表示不设置）
            6.示例行请在导入前删除
            """.formatted(MSG_PIN, MSG_NICKNAME_BLANK, MSG_NICKNAME_LENGTH, MSG_GENDER, MSG_PHONE);

    @ExcelProperty("账号")
    @NotBlank(message = MSG_PIN)
    @Size(min = 5, max = 16, message = MSG_PIN)
    private String pin;

    @ExcelProperty("昵称")
    @NotBlank(message = MSG_NICKNAME_BLANK)
    @Size(max = 20, message = MSG_NICKNAME_LENGTH)
    private String nickname;

    @ExcelProperty("性别")
    @NotBlank(message = MSG_GENDER)
    private String gender;

    @ExcelProperty("手机号")
    @Pattern(regexp = AdminUpdateMyProfileRequest.PHONE_NO_REGEX, message = MSG_PHONE)
    private String phoneNo;

    @ExcelProperty("邮箱")
    @Pattern(regexp = AdminUpdateMyProfileRequest.EMAIL_REGEX, message = MSG_EMAIL)
    @Size(max = 254, message = MSG_EMAIL)
    private String email;

    /**
     * 性别文本转枚举
     */
    public GenderEnum resolveGender() {
        if (CharSequenceUtil.isBlank(gender)) {
            return null;
        }
        for (GenderEnum item : GenderEnum.values()) {
            if (item.getLabel().equals(gender)) {
                return item;
            }
        }
        return null;
    }
}
