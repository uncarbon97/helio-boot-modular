package cc.uncarbon.module.sys.model.valueobj;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 后台管理-登录页，控制前端租户相关 UI
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AdminLoginUIConfigVO implements Serializable {

    @Schema(description = "是否显示租户编码输入框")
    private boolean showTenantCodeInputFlag = false;

}
