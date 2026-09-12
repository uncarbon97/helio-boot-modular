package cc.uncarbon.module.commons.model.request;

import cc.uncarbon.framework.helium.base.enums.BaseEnum;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.module.commons.errorcode.DefaultErrorCodeEnum;
import cn.hutool.core.collection.CollUtil;
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

    /**
     * 当主键ID数组长度大于阈值时，抛出异常
     */
    public void throwIfIdsSizeGt(int threshold) throws BusinessException {
        int size = CollUtil.size(ids);
        if (size > threshold) {
            throw new BusinessException(DefaultErrorCodeEnum.A00003, threshold);
        }
    }
}
