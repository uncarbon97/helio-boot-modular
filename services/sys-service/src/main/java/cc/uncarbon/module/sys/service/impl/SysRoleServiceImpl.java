package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.base.util.StreamFunction;
import cc.uncarbon.framework.helium.db.constant.SQLSegment;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import cc.uncarbon.module.sys.dal.mapper.SysRoleMapper;
import cc.uncarbon.module.sys.enums.SysErrorCodeEnum;
import cc.uncarbon.module.sys.model.interior.UserRoleContainer;
import cc.uncarbon.module.sys.model.query.AdminSysRoleListQuery;
import cc.uncarbon.module.sys.model.request.AdminBindRoleMenuRelationDTO;
import cc.uncarbon.module.sys.model.request.AdminSysRoleUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysRoleDTO;
import cc.uncarbon.module.sys.service.SysMenuService;
import cc.uncarbon.module.sys.service.SysRoleMenuRelationService;
import cc.uncarbon.module.sys.service.SysRoleService;
import cc.uncarbon.module.sys.service.SysUserRoleRelationService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 系统角色
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class SysRoleServiceImpl implements SysRoleService {

    private static final String LOG_PREFIX = "[系统管理][角色管理]";

    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleRelationService sysUserRoleRelationService;
    private final SysRoleMenuRelationService sysRoleMenuRelationService;
    private final SysMenuService sysMenuService;


    @Override
    public PageResult<SysRoleDTO> adminList(AdminSysRoleListQuery query) {
        Set<Long> invisibleRoleIds = determineInvisibleRoleIds();
        Page<SysRoleEntity> entityPage = sysRoleMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<SysRoleEntity>()
                        // 角色编码
                        .like(CharSequenceUtil.isNotBlank(query.getCode()), SysRoleEntity::getCode, CharSequenceUtil.cleanBlank(query.getCode()))
                        // 角色名称
                        .like(CharSequenceUtil.isNotBlank(query.getName()), SysRoleEntity::getName, CharSequenceUtil.cleanBlank(query.getName()))
                        // 不显示特定角色
                        .notIn(CollUtil.isNotEmpty(invisibleRoleIds), SysRoleEntity::getId, invisibleRoleIds)
                        // 排序
                        .orderByDesc(SysRoleEntity::getId)
        );

        return convertPage(entityPage, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long adminCreate(AdminSysRoleUpsertRequest request) {
        log.info(LOG_PREFIX + "新增 >> {}", request);
        preInsertOrUpdateCheck(request);
        checkRepeat(request);

        request.setId(null);
        SysRoleEntity entity = new SysRoleEntity();
        BeanUtil.copyProperties(request, entity);

        sysRoleMapper.insert(entity);

        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminUpdate(AdminSysRoleUpsertRequest request) {
        log.info(LOG_PREFIX + "修改 >> {}", request);
        checkExistence(request.getId());
        preInsertOrUpdateCheck(request);
        checkRepeat(request);

        // 暂不检查该角色是否为当前用户关联的角色

        SysRoleEntity entity = new SysRoleEntity();
        BeanUtil.copyProperties(request, entity);

        sysRoleMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDelete(Collection<Long> ids) {
        log.info(LOG_PREFIX + "删除 >> {}", ids);
        preDeleteCheck(ids);
        sysRoleMapper.deleteByIds(ids);
    }

    @Override
    public SysRoleDTO getById(Long id) {
        SysRoleEntity entity = sysRoleMapper.selectById(id);
        return convertEntity(entity, true);
    }

    @Override
    public SysRoleDTO getNonnullById(Long id) throws NoRecordException {
        return NoRecordException.throwIfNull(getById(id));
    }

    /**
     * 后台管理-绑定角色与菜单关联关系
     *
     * @return 新菜单ID集合对应的权限名
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Set<String> adminBindMenus(AdminBindRoleMenuRelationDTO dto) {
        preBindRoleMenuRelationCheck(dto);
        Set<String> newPermissions = sysMenuService.listPermissionsByMenuIds(dto.getMenuIds());
        sysRoleMenuRelationService.cleanAndBind(dto.getRoleId(), dto.getMenuIds());

        return newPermissions;
    }

    /**
     * 后台管理-下拉框数据
     */
    @Override
    public List<SysRoleDTO> adminSelectOptions() {
        Set<Long> invisibleRoleIds = determineInvisibleRoleIds();
        List<SysRoleEntity> entityList = sysRoleMapper.selectList(
                new QueryWrapper<SysRoleEntity>()
                        .lambda()
                        // 只取特定字段
                        .select(SysRoleEntity::getId, SysRoleEntity::getName)
                        // 不显示特定角色
                        .notIn(CollUtil.isNotEmpty(invisibleRoleIds), SysRoleEntity::getId, invisibleRoleIds)
                        // 排序
                        .orderByAsc(SysRoleEntity::getId)
        );
        // 无需填充菜单IDs
        return convertList(entityList, false);
    }

    /**
     * 后台管理-删除指定租户的特定角色
     *
     * @param tenantIds 租户IDs，非主键ID，必填
     * @param roleCodes 角色值集合，可以为空
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDeleteTenantRoles(Collection<Long> tenantIds, Collection<String> roleCodes) {
        if (CollUtil.isEmpty(tenantIds)) {
            return;
        }

        sysRoleMapper.delete(
                new QueryWrapper<SysRoleEntity>()
                        .lambda()
                        // 租户ID
                        .in(SysRoleEntity::getTenantId, tenantIds)
                        // 值相符
                        .in(CollUtil.isNotEmpty(roleCodes), SysRoleEntity::getCode, roleCodes)
        );
    }

    /**
     * 取用户ID拥有角色对应的 角色ID-角色名 map
     *
     * @param userId 用户ID
     * @return 失败返回空 map
     */
    @Override
    public Map<Long, String> getRoleMapByUserId(Long userId) {
        Set<Long> roleIds = sysUserRoleRelationService.listRoleIdsByUser(userId);

        if (CollUtil.isEmpty(roleIds)) {
            return Map.of();
        }

        // 根据角色Ids取 map
        return sysRoleMapper.selectList(
                new QueryWrapper<SysRoleEntity>()
                        .lambda()
                        .select(SysRoleEntity::getId, SysRoleEntity::getCode)
                        .in(SysRoleEntity::getId, roleIds)
        ).stream().collect(Collectors.toMap(SysRoleEntity::getId, SysRoleEntity::getCode, StreamFunction.keepExisting()));
    }

    /**
     * 取当前用户关联角色信息
     * 仅内部使用
     */
    @Override
    public UserRoleContainer getCurrentUserRoleContainer() {
        return getSpecifiedUserRoleContainer(UserContextHolder.getUserId());
    }

    /**
     * 取指定用户关联角色信息
     * 仅内部使用
     */
    @Override
    public UserRoleContainer getSpecifiedUserRoleContainer(Long specifiedUserId) {
        Set<Long> userRoleIds = sysUserRoleRelationService.listRoleIdsByUser(specifiedUserId);
        List<SysRoleEntity> userRoles = null;
        if (CollUtil.isNotEmpty(userRoleIds)) {
            userRoles = sysRoleMapper.selectByIds(userRoleIds);
        }
        if (CollUtil.isEmpty(userRoles)) {
            userRoles = List.of();
        }
        return new UserRoleContainer(userRoleIds, userRoles);
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    /**
     * 实体转值对象
     *
     * @param fillMenu 是否填充菜单
     */
    private SysRoleDTO convertEntity(SysRoleEntity entity, boolean fillMenu) {
        if (entity == null) {
            return null;
        }

        SysRoleDTO ret = new SysRoleDTO();
        BeanUtil.copyProperties(entity, ret);
        // 按需改写字段
        if (fillMenu) {
            ret.setMenuIds(sysRoleMenuRelationService.listMenuIdsByRoleIds(Collections.singleton(ret.getId())));
        }

        return ret;
    }

    /**
     * 实体转值对象
     *
     * @param fillMenu 是否填充菜单
     */
    private List<SysRoleDTO> convertList(List<SysRoleEntity> entityList, boolean fillMenu) {
        // 深拷贝
        List<SysRoleDTO> ret = new ArrayList<>(entityList.size());
        entityList.forEach(
                entity -> ret.add(convertEntity(entity, fillMenu))
        );

        return ret;
    }

    /**
     * 实体转值对象
     *
     * @param fillMenu 是否填充菜单
     */
    private PageResult<SysRoleDTO> convertPage(Page<SysRoleEntity> entityPage, boolean fillMenu) {
        return new PageResult<SysRoleDTO>()
                .setCurrent(entityPage.getCurrent())
                .setSize(entityPage.getSize())
                .setTotal(entityPage.getTotal())
                .setRecords(convertList(entityPage.getRecords(), fillMenu));
    }

    /**
     * 检查是否存在
     */
    private void checkExistence(Long id) {
        boolean exists = sysRoleMapper.exists(
                new LambdaQueryWrapper<SysRoleEntity>()
                        .select(SysRoleEntity::getId)
                        .eq(SysRoleEntity::getId, id)
                        .last(SQLSegment.LIMIT_1)
        );
        NoRecordException.throwIfFalse(exists);
    }

    /**
     * 检查是否存在重复
     */
    private void checkRepeat(AdminSysRoleUpsertRequest request) {
        SysRoleEntity existingEntity = sysRoleMapper.selectOne(
                new QueryWrapper<SysRoleEntity>()
                        .lambda()
                        // 仅取主键ID
                        .select(SysRoleEntity::getId)
                        // 名称相同
                        .eq(SysRoleEntity::getName, request.getName())
                        .last(SQLSegment.LIMIT_1)
        );

        if (existingEntity != null && !existingEntity.getId().equals(request.getId())) {
            throw new BusinessException(400, "已存在相同系统角色，请重新输入");
        }

        if (request.creatingNewTenantAdmin()) {
            long qty = sysRoleMapper.selectCount(
                    new QueryWrapper<SysRoleEntity>()
                            .lambda()
                            // 租户ID相同
                            .eq(SysRoleEntity::getTenantId, request.getTenantId())
                            // 角色编码相同
                            .eq(SysRoleEntity::getCode, request.getCode())
                            .last(SQLSegment.LIMIT_1)
            );
            SysErrorCodeEnum.NEED_DELETE_EXISTING_TENANT_ADMIN_ROLE.assertTrue(qty <= 0L, request.getTenantId());
        }
    }

    /**
     * 确定不可见角色IDs
     * 租户管理员：列表中不显示超级管理员角色
     * 普通角色：列表中不显示超级管理员、租户管理员角色
     *
     * @return mutable Set，支持外部改变元素
     */
    private Set<Long> determineInvisibleRoleIds() {
        UserRoleContainer currentUser = getCurrentUserRoleContainer();
        // 超级管理员：不限制
        if (currentUser.isSuperAdmin()) {
            return new HashSet<>();
        }
        // 租户管理员：列表中不显示超级管理员角色
        if (currentUser.isTenantAdmin()) {
            return CollUtil.newHashSet(SysConstant.SUPER_ADMIN_ROLE_ID);
        }
        // 普通角色：列表中不显示超级管理员、租户管理员角色
        Set<Long> ret = sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRoleEntity>()
                        // 仅取主键ID
                        .select(SysRoleEntity::getId)
                        // 值相符
                        .eq(SysRoleEntity::getCode, SysConstant.TENANT_ADMIN_ROLE_VALUE)
        ).stream().map(SysRoleEntity::getId).collect(Collectors.toSet());
        ret.add(SysConstant.SUPER_ADMIN_ROLE_ID);
        return ret;
    }

    /**
     * 新增/修改系统角色信息前检查
     */
    private void preInsertOrUpdateCheck(AdminSysRoleUpsertRequest request) {
        if (SysConstant.SUPER_ADMIN_ROLE_VALUE.equalsIgnoreCase(request.getCode())) {
            // 角色编码不能为SuperAdmin
            throw new BusinessException(SysErrorCodeEnum.A01010, SysConstant.SUPER_ADMIN_ROLE_VALUE);
        }
        if (SysConstant.TENANT_ADMIN_ROLE_VALUE.equalsIgnoreCase(request.getCode()) && !request.creatingNewTenantAdmin()) {
            // 除非是新增租户时，同时新增租户管理员角色，否则角色编码不能为Admin
            throw new BusinessException(SysErrorCodeEnum.A01010, SysConstant.TENANT_ADMIN_ROLE_VALUE);
        }

        boolean isUpdating = Objects.nonNull(request.getId());
        if (isUpdating) {
            SysRoleEntity existingRole = sysRoleMapper.selectById(request.getId());
            SysErrorCodeEnum.A01001.assertNotNull(existingRole);
            if (existingRole.isSuperAdmin() || existingRole.isTenantAdmin()) {
                // 原来角色编码为SuperAdmin或Admin的，不能被改变
                throw new BusinessException(SysErrorCodeEnum.A01010, existingRole.getCode());
            }
        }
    }

    /**
     * 删除系统角色前检查
     */
    private void preDeleteCheck(Collection<Long> ids) {
        if (CollUtil.contains(ids, SysConstant.SUPER_ADMIN_ROLE_ID)) {
            throw new BusinessException(SysErrorCodeEnum.A01011);
        }

        List<SysRoleEntity> existingEntityList = sysRoleMapper.selectByIds(ids);
        for (SysRoleEntity item : existingEntityList) {
            if (item.isSuperAdmin()) {
                throw new BusinessException(SysErrorCodeEnum.A01011);
            }

            if (item.isTenantAdmin()) {
                throw new BusinessException(SysErrorCodeEnum.CANNOT_DELETE_TENANT_ADMIN_ROLE);
            }
        }

        UserRoleContainer currentUser = getCurrentUserRoleContainer();
        if (CollUtil.containsAny(currentUser.getRelatedRoleIds(), ids)) {
            throw new BusinessException(SysErrorCodeEnum.A01012);
        }
    }

    /**
     * 绑定系统角色与菜单关联关系前检查
     * 防止越权访问漏洞
     */
    private void preBindRoleMenuRelationCheck(AdminBindRoleMenuRelationDTO dto) {
        UserRoleContainer currentUser = getCurrentUserRoleContainer();
        if (SysConstant.SUPER_ADMIN_ROLE_ID.equals(dto.getRoleId())) {
            throw new BusinessException(SysErrorCodeEnum.A01013);
        }

        if (CollUtil.contains(currentUser.getRelatedRoleIds(), dto.getRoleId())) {
            // 不能动自身角色
            throw new BusinessException(SysErrorCodeEnum.A01014);
        }

        // 有且只有当前用户为超级管理员，才可以为租户管理员绑定菜单
        SysRoleEntity targetRole = sysRoleMapper.selectById(dto.getRoleId());
        if (targetRole.isTenantAdmin() && !currentUser.isSuperAdmin()) {
            throw new BusinessException(SysErrorCodeEnum.CANNOT_BIND_MENUS_FOR_TENANT_ADMIN_ROLE);
        }

        if (CollUtil.isNotEmpty(dto.getMenuIds()) && !currentUser.isSuperAdmin()) {
            // 超级管理员之外的角色，都需要校验自身菜单范围是否满足输入值
            Set<Long> visibleMenuIds = sysRoleMenuRelationService.listMenuIdsByRoleIds(currentUser.getRelatedRoleIds());
            if (!CollUtil.containsAll(visibleMenuIds, dto.getMenuIds())) {
                // 可能存在超自身权限赋权
                throw new BusinessException(SysErrorCodeEnum.A01015);
            }
        }
    }
}
