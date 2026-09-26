package cc.uncarbon.module.commons.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 返回单个主键 ID，兼容任意类型的主键
 *
 * @param <T> 主键数据类型
 * @author Uncarbon
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class IdResponse<T extends Serializable> implements Serializable {

    @Schema(description = "ID")
    private T id;

}
