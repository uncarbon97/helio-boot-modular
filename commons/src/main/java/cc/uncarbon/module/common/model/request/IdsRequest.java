package cc.uncarbon.module.common.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Collection;

/**
 * 同时接收多个主键 ID，兼容 Integer / Long / String 等多种类型的主键
 * 使用时，根据需要手动加上 @RequestBody @Valid 等注解
 *
 * @param <T> 主键数据类型
 * @author Uncarbon
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class IdsRequest<T extends Serializable> implements Serializable {

    @Schema(description = "主键ID数组", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "主键ID数组必填")
    private Collection<T> ids;

}
