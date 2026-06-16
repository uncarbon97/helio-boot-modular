package cc.uncarbon.module.commons.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 接收单个主键 ID，兼容任意类型的主键
 * 使用时，根据需要手动加上 @RequestBody @Valid 等注解
 *
 * @param <T> 主键数据类型
 * @author Uncarbon
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class IdRequest<T extends Serializable> implements Serializable {

    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "ID必填")
    private T id;

}
