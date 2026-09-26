package cc.uncarbon.module.commons.model.request;

import cc.uncarbon.framework.helium.base.enums.BaseEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 修改状态请求
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminSetStatusRequest<I extends Serializable, E extends BaseEnum<?>> implements Serializable {

    @Schema(description = "主键ID")
    @NotNull(message = "主键ID必填")
    private I id;

    @Schema(description = "新状态")
    @NotNull(message = "新状态必填")
    private E newStatus;
}
