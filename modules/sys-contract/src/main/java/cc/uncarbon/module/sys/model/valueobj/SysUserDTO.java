package cc.uncarbon.module.sys.model.valueobj;

import cc.uncarbon.framework.helium.db.enums.GenderEnum;
import cc.uncarbon.module.commons.model.valueobj.AuditUserDisplayAware;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.Instant;


/**
 * 系统用户
 *
 * <p>列表场景以 {@code phoneNoMasked}/{@code emailMasked} 为准（零解密直读），
 * 完整明文仅详情/导出接口返回；明文字段 @ToString.Exclude 不进日志</p>
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SysUserDTO implements Serializable, AuditUserDisplayAware {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "创建时刻")
    private Instant createdAt;

    @Schema(description = "创建者")
    private String createdBy;

    @Schema(description = "更新时刻")
    private Instant updatedAt;

    @Schema(description = "更新者")
    private String updatedBy;

    @Schema(description = "账号")
    private String pin;

    @Schema(description = "昵称")
    @ToString.Exclude
    private String nickname;

    @Schema(description = "状态")
    private SysUserStatusEnum status;

    @Schema(description = "性别")
    private GenderEnum gender;

    @Schema(description = "邮箱（仅详情返回明文；列表恒为 null）")
    @ToString.Exclude
    private String email;

    @Schema(description = "手机号（仅详情返回明文；列表恒为 null）")
    @ToString.Exclude
    private String phoneNo;

    @Schema(description = "手机号脱敏展示值，如 189****8927；列表场景以此为准")
    private String phoneNoMasked;

    @Schema(description = "邮箱脱敏展示值，如 al***@example.com；列表场景以此为准")
    private String emailMasked;

    @Schema(description = "最后登录时刻")
    private Instant lastLoginAt;

    @Schema(description = "所属部门ID")
    private Long deptId;

    @Schema(description = "所属部门名称")
    private String deptName;

    @Schema(description = "头像URL")
    private String avatarUrl;

}
