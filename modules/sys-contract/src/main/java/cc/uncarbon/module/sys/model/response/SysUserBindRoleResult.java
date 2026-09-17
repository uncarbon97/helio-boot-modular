package cc.uncarbon.module.sys.model.response;

import cc.uncarbon.module.sys.model.valueobj.SysUserDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;


/**
 * 系统用户-绑定角色结果
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SysUserBindRoleResult implements Serializable {

    @Schema(description = "用户信息")
    private SysUserDTO old;

    @Schema(description = "最新关联角色名称")
    private List<String> roleNames;

}
