package cc.uncarbon.module.sys.model.valueobj;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;


/**
 * 系统角色BO
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SysRoleBO implements Serializable {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "创建时刻")
    private LocalDateTime createdAt;

    @Schema(description = "更新时刻")
    private LocalDateTime updatedAt;

    @Schema(description = "角色编码")
    private String code;

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "可见菜单Ids")
    private Collection<Long> menuIds;

}
