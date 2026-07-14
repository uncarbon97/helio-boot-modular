package cc.uncarbon.module.adminapi.controller.selectoption;

import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.adminapi.constant.AdminPermissionConstant;
import cc.uncarbon.module.adminapi.model.response.AdminSelectOptionItemVO;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.satoken.StpKit;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.sys.model.valueobj.SysDeptDTO;
import cc.uncarbon.module.sys.model.valueobj.SysRoleDTO;
import cc.uncarbon.module.sys.service.SysDeptService;
import cc.uncarbon.module.sys.service.SysRoleService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 这里统一存放所有用于后台管理的下拉框数据源接口
 * 避免多人协作时，不知道原来是否已经有了，或者写在某个边边角角里，造成重复开发
 * <p>
 * {@code @SaCheckLogin(type = StpLoginType.ADMIN)} 表示只有登录后才能请求
 */
@Tag(name = "后台管理-#下拉框数据源接口")
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/list-select-option")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminSelectOptionController {

    private final SysRoleService sysRoleService;
    private final SysDeptService sysDeptService;


    @SaCheckLogin(type = StpLoginType.ADMIN)
    @Operation(summary = "系统角色下拉框")
    @PostMapping(value = "/sys/role")
    public ApiResult<List<AdminSelectOptionItemVO>> role() {
        return ApiResult.success(AdminSelectOptionItemVO.ofCollection(sysRoleService.adminListSelectOption(),
                SysRoleDTO::getId, SysRoleDTO::getName)
        );
    }

    @SaCheckLogin(type = StpLoginType.ADMIN)
    @Operation(summary = "部门下拉框")
    @PostMapping(value = "/sys/dept")
    public ApiResult<List<AdminSelectOptionItemVO>> dept() {
        // true = 让高级 HR 等角色可以调整用户部门，那就需要 TA 可以看到所有部门
        // false = 只能看到本部门及以下
        boolean hasBindDeptPerm = StpKit.ADMIN.hasPermission(AdminPermissionConstant.BIND_DEPT);
        return ApiResult.success(AdminSelectOptionItemVO.ofCollection(sysDeptService.adminListSelectOption(!hasBindDeptPerm),
                SysDeptDTO::getId, SysDeptDTO::getName, SysDeptDTO::getParentId)
        );
    }

}
