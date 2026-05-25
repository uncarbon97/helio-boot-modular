package cc.uncarbon.module.sys.model.request;

import cc.uncarbon.framework.core.enums.EnabledStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 系统管理-分页列表租户主数据
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminListTenantMetaDTO implements Serializable {

    @Schema(description = "租户名称(关键词)")
    private String name;

    @Schema(description = "租户ID(纯数字)")
    private Long id;

    @Schema(description = "租户编码(关键词)")
    private String code;

    @Schema(description = "状态")
    private EnabledStatusEnum status;

}
