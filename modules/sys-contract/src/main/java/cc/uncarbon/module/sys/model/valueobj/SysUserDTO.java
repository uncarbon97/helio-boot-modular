package cc.uncarbon.module.sys.model.valueobj;

import cc.uncarbon.framework.helium.db.enums.GenderEnum;
import cc.uncarbon.module.commons.model.valueobj.AuditUserDisplayAware;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.Instant;


/**
 * 系统用户
 *
 * <p>列表场景以 {@code phoneNoMasked}/{@code emailMasked} 为准（零解密直读），
 * 完整明文仅详情/导出接口返回；明文字段 @ToString.Exclude 不进日志</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
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
    private String nickname;

    @Schema(description = "性别")
    private GenderEnum gender;

    @Schema(description = "状态")
    private SysUserStatusEnum status;

    @Schema(description = "最后登录时刻")
    private Instant lastLoginAt;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "手机号脱敏")
    private String phoneNoMasked;

    @Schema(description = "邮箱脱敏")
    private String emailMasked;

    @Schema(description = "所属部门ID")
    private Long deptId;

    @Schema(description = "所属部门名称")
    private String deptName;

}
