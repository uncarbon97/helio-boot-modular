package cc.uncarbon.module.sys.service;

import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.commons.model.request.AdminSetStatusRequest;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import cc.uncarbon.module.sys.model.query.AdminSysRoleListRelatedUserQuery;
import cc.uncarbon.module.sys.model.query.AdminSysUserListQuery;
import cc.uncarbon.module.sys.model.request.*;
import cc.uncarbon.module.sys.model.response.SysUserBindRoleResult;
import cc.uncarbon.module.sys.model.response.SysUserImportResult;
import cc.uncarbon.module.sys.model.response.TenantUserCreateResult;
import cc.uncarbon.module.sys.model.valueobj.SysUserBasicProfileDTO;
import cc.uncarbon.module.sys.model.valueobj.SysUserDTO;
import org.jspecify.annotations.NonNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

public interface SysUserService {

    /**
     * 后台管理-分页查询
     */
    PageResult<SysUserDTO> adminList(AdminSysUserListQuery query);

    /**
     * 后台管理-分页查询指定角色关联的用户
     */
    PageResult<SysUserDTO> adminListRoleRelatedUsers(AdminSysRoleListRelatedUserQuery query);

    /**
     * 后台管理-新增
     *
     * @return 主键ID
     */
    Long adminCreate(AdminSysUserCreateRequest request);

    /**
     * 后台管理-修改
     */
    void adminUpdate(AdminSysUserUpdateRequest request);

    /**
     * 后台管理-修改状态
     */
    void adminSetStatus(AdminSetStatusRequest<Long, SysUserStatusEnum> request);

    /**
     * 后台管理-删除
     */
    void adminDelete(Collection<Long> ids);

    /**
     * 取当前用户可操作的指定用户详情，无权限会抛出业务异常
     */
    SysUserDTO getOperableById(Long id);

    /**
     * 根据 ID 取当前用户可操作的指定用户详情，未取到会抛出 {@link NoRecordException}
     */
    SysUserDTO getNonnullOperableById(Long id) throws NoRecordException;

    /**
     * 根据 ID 取用户基本信息，未取到会抛出 {@link NoRecordException}
     */
    SysUserBasicProfileDTO getNonnullBasicProfileById(Long id) throws NoRecordException;

    /**
     * 新增租户用户
     */
    TenantUserCreateResult createTenantUser(TenantUserCreateRequest request);

    /**
     * 后台管理-重置密码
     */
    void adminResetPassword(AdminSysUserResetPasswordRequest request);

    /**
     * 后台管理-绑定用户角色
     */
    SysUserBindRoleResult adminBindRole(AdminSysUserBindRoleRequest request);

    /**
     * 后台管理-调整用户部门
     */
    void adminBindDept(AdminSysUserBindDeptRequest request);

    /**
     * 校验当前用户是否可查询目标用户的关联信息（防止跨租户/越权 ID 枚举）
     */
    void checkUserQueryAccess(Long userId);

    /**
     * 批量校验当前用户是否可查询目标用户的关联信息（防止跨租户/越权 ID 枚举）
     */
    void checkUserQueryAccess(Collection<Long> userIds);

    /**
     * 校验当前用户是否可查询目标角色的关联信息（防止跨租户/越权 ID 枚举）
     */
    void checkRoleQueryAccess(Long roleId);

    /**
     * 后台管理-生成用户导入模板 xlsx
     *
     * @param out 输出流（调用方负责关闭）
     */
    void adminWriteImportTemplate(OutputStream out);

    /**
     * 后台管理-批量导入用户（xlsx）
     * <p>文件级 all-or-nothing：任一行校验失败则整文件不入库</p>
     *
     * @param excelStream      上传的 xlsx 文件流（调用方负责关闭）
     * @param originalFilename 原始文件名
     * @param attr             统一导入参数
     */
    SysUserImportResult adminImportUsers(InputStream excelStream, String originalFilename, @NonNull AdminSysUserImportRequest attr);

}
