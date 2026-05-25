package cc.uncarbon.module.adminapi.controller.common;

import cc.uncarbon.framework.web.model.reponse.ApiResult;
import cc.uncarbon.module.adminapi.constant.AdminApiConstant;
import cc.uncarbon.module.adminapi.model.response.AdminSelectOptionItemVO;
import cc.uncarbon.module.adminapi.util.AdminStpUtil;
import cc.uncarbon.module.sys.model.response.SysDeptDTO;
import cc.uncarbon.module.sys.model.response.SysRoleBO;
import cc.uncarbon.module.sys.service.impl.SysDeptServiceImpl;
import cc.uncarbon.module.sys.service.SysRoleService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 约束：登录后才能使用   👇 系统管理对应的鉴权工具类
@SaCheckLogin(type = StpLoginType.ADMIN)
@Tag(name = "系统管理-下拉框数据源接口")
@RequestMapping(AdminApiConstant.HTTP_API_URL_PREFIX + "/api/v1")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminSelectOptionsController {

    private final SysRoleService sysRoleService;
    private final SysDeptServiceImpl sysDeptService;


    /*
    这里统一存放所有用于系统管理的下拉框数据源接口
    避免多人协作时，不知道原来是否已经有了，或者写在某个边边角角里，造成重复开发
    */

    @Operation(summary = "系统角色下拉框")
    @GetMapping(value = "/select-options/roles")
    public ApiResult<List<AdminSelectOptionItemVO>> roles() {
        return ApiResult.success(
                AdminSelectOptionItemVO.listOf(sysRoleService.adminSelectOptions(), SysRoleBO::getId, SysRoleBO::getTitle)
        );
    }

    @Operation(summary = "部门下拉框（前端负责转为树状数据）")
    @GetMapping(value = "/select-options/depts")
    public ApiResult<List<AdminSelectOptionItemVO>> depts() {
        return ApiResult.success(
                AdminSelectOptionItemVO.listOf(sysDeptService.adminSelectOptions(true), SysDeptDTO::getId, SysDeptDTO::getTitle, SysDeptDTO::getParentId)
        );
    }

}
