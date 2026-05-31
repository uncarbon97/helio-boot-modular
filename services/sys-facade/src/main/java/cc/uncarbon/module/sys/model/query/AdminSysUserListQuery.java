package cc.uncarbon.module.sys.model.query;

import cc.uncarbon.framework.helium.base.page.PageParam;
import cc.uncarbon.framework.helium.base.page.PageQuery;
import cc.uncarbon.module.sys.constant.SysConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Objects;


/**
 * 系统管理-分页查询后台用户
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminSysUserListQuery implements Serializable, PageQuery {

    @Schema(description = "分页查询参数")
    private PageParam pageParam;

    @Schema(description = "手机号(关键词)")
    private String phoneNo;

    @Schema(description = "手动选择的部门ID")
    private Long selectedDeptId;

    /**
     * 是否需要根据【手动选择的部门】筛选用户
     */
    public boolean needFilterBySelectedDeptId() {
        return Objects.nonNull(selectedDeptId) && selectedDeptId > SysConstant.ROOT_PARENT_ID;
    }

}
