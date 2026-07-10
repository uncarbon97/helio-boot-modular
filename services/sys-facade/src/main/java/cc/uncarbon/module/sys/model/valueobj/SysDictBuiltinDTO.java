package cc.uncarbon.module.sys.model.valueobj;

import cc.uncarbon.module.commons.enumdict.EnumDict;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;


/**
 * 字典分类 - 由 {@link EnumDict} 注解生成的内置字典
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SysDictBuiltinDTO implements Serializable {

    @Schema(description = "字典编码")
    private String code;

    @Schema(description = "字典名称")
    private String name;

    @Schema(description = "字典描述")
    private String description;

    @Schema(description = "字典项集合")
    private List<SysDictItemDTO> items;

}
