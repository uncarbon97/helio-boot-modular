package cc.uncarbon.module.common.model.request;

import cc.uncarbon.framework.helio.base.enums.BaseEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Collection;

/**
 * 批量修改状态请求
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminBatchSetStatusRequest<I extends Serializable, E extends BaseEnum<?>> implements Serializable {

    @Schema(description = "主键ID数组")
    @NotEmpty(message = "主键ID数组必填")
    private Collection<I> ids;

    @Schema(description = "新状态")
    @NotNull(message = "新状态必填")
    private E newStatus;

}
