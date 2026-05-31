package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.core.constant.HeliumConstant;
import cc.uncarbon.framework.core.context.UserContextHolder;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.core.function.StreamFunction;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.db.constant.SQLSegment;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import cc.uncarbon.module.sys.enums.SysErrorCodeEnum;
import cc.uncarbon.module.sys.dal.mapper.SysRoleMapper;
import cc.uncarbon.module.sys.model.interior.UserRoleContainer;
import cc.uncarbon.module.sys.model.request.AdminBindRoleMenuRelationDTO;
import cc.uncarbon.module.sys.model.request.AdminSysRoleUpsertRequest;
import cc.uncarbon.module.sys.model.query.AdminSysRoleListQuery;
import cc.uncarbon.module.sys.model.valueobj.SysRoleBO;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cc.uncarbon.module.sys.service.SysMenuService;
import cc.uncarbon.module.sys.service.SysRoleMenuRelationService;
import cc.uncarbon.module.sys.service.SysRoleService;
import cc.uncarbon.module.sys.service.SysUserRoleRelationService;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 系统角色
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleRelationService sysUserRoleRelationService;
    private final SysRoleMenuRelationService sysRoleMenuRelationService;
    private final SysMenuService sysMenuService;


    /**
     * 系统管理-分页查询
     */
    @Override
    public PageResult<SysRoleBO> adminList(AdminSysRoleListQuery query) {
        Set<Long> invisibleRoleIds = determineInvisibleRoleIds();
        Page<SysRoleEntity> entityPage = sysRoleMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new QueryWrapper<SysRoleEntity>()
                        .lambda()
                        // 名称
                        .like(CharSequenceUtil.isNotBlank(query.getTitle()), SysRoleEntity::getTitle, CharSequenceUtil.cleanBlank(query.getTitle()))
                        // 值
                        .like(CharSequenceUtil.isNotBlank(query.getValue()), SysRoleEntity::getValue, CharSequenceUtil.cleanBlank(query.getValue()))
                        // 不显示特定角色
                        .notIn(CollUtil.isNotEmpty(invisibleRoleIds), SysRoleEntity::getId, invisibleRoleIds)
                        // 排序
                        .orderByDesc(SysRoleEntity::getId)
        );

        return this.entityPage2BOPage(entityPage, true);
    }

    /**
     * 根据 ID 取详情
     *
     * @param id 主键ID
     * @return null or 详情
     */
    @Override
    public SysRoleBO getOneById(Long id) {
        return this.getOneById(id, false);
    }

    /**
     * 根据 ID 取详情
     *
     * @param id 主键ID
     * @param throwIfInvalidId 未找到时是否抛出异常
     * @return null or 详情
     */
    @Override
    public SysRoleBO getOneById(Long id, boolean throwIfInvalidId) throws BusinessException {
        SysRoleEntity entity = sysRoleMapper.selectById(id);
        if (throwIfInvalidId) {
            SysErrorCodeEnum.A01001.assertNotNull(entity);
        }

        return this.entity2BO(entity, true);
    }

    /**
     * 系统管理-新增
     *
     * @return 主键ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long adminCreate(AdminSysRoleUpsertRequest request) {
        log.info("[系统管理-新增系统角色] >> 入参={}", request);
        preInsertOrUpdateCheck(request);
        checkRepeat(request);

        request.setId(null);
        SysRoleEntity entity = new SysRoleEntity();
        BeanUtil.copyProperties(request, entity);

        sysRoleMapper.insert(entity);

        return entity.getId();
    }

    /**
     * 系统管理-修改
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminUpdate(AdminSysRoleUpsertRequest request) {
        log.info("[系统管理-修改系统角色] >> 入参={}", request);
        preInsertOrUpdateCheck(request);
        checkRepeat(request);

        // 暂不检查该角色是否为当前用户关联的角色

        SysRoleEntity entity = new SysRoleEntity();
        BeanUtil.copyProperties(request, entity);

        sysRoleMapper.updateById(entity);
    }

    /**
     * 系统管理-删除
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDelete(Collection<Long> ids) {
        log.info("[系统管理-删除系统角色] >> 入参={}", ids);
        preDeleteCheck(ids);
        sysRoleMapper.deleteByIds(ids);
    }

    /**
     * 系统管理-绑定角色与菜单关联关系
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
     * 系统管理-下拉框数据
     */
    @Override
    public List<SysRoleBO> adminSelectOptions() {
        Set<Long> invisibleRoleIds = determineInvisibleRoleIds();
        List<SysRoleEntity> entityList = sysRoleMapper.selectList(
                new QueryWrapper<SysRoleEntity>()
                        .lambda()
                        // 只取特定字段
                        .select(SysRoleEntity::getId, SysRoleEntity::getTitle)
                        // 不显示特定角色
                        .notIn(CollUtil.isNotEmpty(invisibleRoleIds), SysRoleEntity::getId, invisibleRoleIds)
                        // 排序
                        .orderByAsc(SysRoleEntity::getId)
        );
        // 无需填充菜单IDs
        return entityList2BOs(entityList, false);
    }

    /**
     * 系统管理-删除指定租户的特定角色
     * @param tenantIds 租户IDs，非主键ID，必填
     * @param roleValues 角色值集合，可以为空
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDeleteTenantRoles(Collection<Long> tenantIds, Collection<String> roleValues) {
        if (CollUtil.isEmpty(tenantIds)) {
            return;
        }

        sysRoleMapper.delete(
                new QueryWrapper<SysRoleEntity>()
                        .lambda()
                        // 租户ID
                        .in(SysRoleEntity::getTenantId, tenantIds)
                        // 值相符
                        .in(CollUtil.isNotEmpty(roleValues), SysRoleEntity::getValue, roleValues)
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
        Set<Long> roleIds = sysUserRoleRelationService.listRoleIdsByUserId(userId);

        if (CollUtil.isEmpty(roleIds)) {
            return Map.of();
        }

        // 根据角色Ids取 map
        return sysRoleMapper.selectList(
                new QueryWrapper<SysRoleEntity>()
                        .lambda()
                        .select(SysRoleEntity::getId, SysRoleEntity::getValue)
                        .in(SysRoleEntity::getId, roleIds)
        ).stream().collect(Collectors.toMap(SysRoleEntity::getId, SysRoleEntity::getValue, StreamFunction.ignoredThrowingMerger()));
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
        Set<Long> userRoleIds = sysUserRoleRelationService.listRoleIdsByUserId(specifiedUserId);
        List<SysRoleEntity> userRoles = null;
        if (CollUtil.isNotEmpty(userRoleIds)) {
            userRoles = sysRoleMapper.selectBatchIds(userRoleIds);
        }
        if (CollUtil.isEmpty(userRoles)) {
            userRoles = Collections.emptyList();
        }
        return new UserRoleContainer(userRoleIds, userRoles);
    }

    /**
     * 确定不可见角色IDs
     * 仅内部使用
     * 租户管理员：列表中不显示超级管理员角色
     * 普通角色：列表中不显示超级管理员、租户管理员角色
     * @return mutable Set，支持外部改变元素
     */
    @Override
    public Set<Long> determineInvisibleRoleIds() {
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
                new QueryWrapper<SysRoleEntity>()
                        .lambda()
                        // 仅取主键ID
                        .select(SysRoleEntity::getId)
                        // 值相符
                        .eq(SysRoleEntity::getValue, SysConstant.TENANT_ADMIN_ROLE_VALUE)
        ).stream().map(SysRoleEntity::getId).collect(Collectors.toSet());
        ret.add(SysConstant.SUPER_ADMIN_ROLE_ID);
        return ret;
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    /**
     * 实体转值对象
     * @param fillMenuIds 是否根据实体ID，查询关联菜单IDs并填充到BO
     */
    private SysRoleBO entity2BO(SysRoleEntity entity, boolean fillMenuIds) {
        if (entity == null) {
            return null;
        }

        SysRoleBO bo = new SysRoleBO();
        BeanUtil.copyProperties(entity, bo);

        // 按需改写字段
        if (fillMenuIds) {
            bo.setMenuIds(sysRoleMenuRelationService.listMenuIdsByRoleIds(Collections.singleton(bo.getId())));
        }
        return bo;
    }

    /**
     * 实体转值对象
     *
     * @param entityList 实体 List
     * @param fillMenuIds 是否根据实体ID，查询关联菜单IDs并填充到BO List
     */
    private List<SysRoleBO> entityList2BOs(List<SysRoleEntity> entityList, boolean fillMenuIds) {
        // 深拷贝
        List<SysRoleBO> ret = new ArrayList<>(entityList.size());
        entityList.forEach(
                entity -> ret.add(this.entity2BO(entity, fillMenuIds))
        );

        return ret;
    }

    /**
     * 实体转值对象
     *
     * @param entityPage 实体分页
     * @param fillMenuIds 是否根据实体ID，查询关联菜单IDs并填充到BO 分页
     */
    private PageResult<SysRoleBO> entityPage2BOPage(Page<SysRoleEntity> entityPage, boolean fillMenuIds) {
        return new PageResult<SysRoleBO>()
                .setCurrent(entityPage.getCurrent())
                .setSize(entityPage.getSize())
                .setTotal(entityPage.getTotal())
                // 需填充菜单IDs
                .setRecords(this.entityList2BOs(entityPage.getRecords(), fillMenuIds));
    }

    /**
     * 检查是否存在重复
     *
     * @param dto DTO
     */
    private void checkRepeat(AdminSysRoleUpsertRequest request) {
        SysRoleEntity existingEntity = sysRoleMapper.selectOne(
                new QueryWrapper<SysRoleEntity>()
                        .lambda()
                        // 仅取主键ID
                        .select(SysRoleEntity::getId)
                        // 名称相同
                        .eq(SysRoleEntity::getTitle, request.getTitle())
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
                            .eq(SysRoleEntity::getValue, request.getValue())
                            .last(SQLSegment.LIMIT_1)
            );
            SysErrorCodeEnum.NEED_DELETE_EXISTING_TENANT_ADMIN_ROLE.assertTrue(qty <= 0L, request.getTenantId());
        }
    }

    /**
     * 新增/修改系统角色信息前检查
     */
    private void preInsertOrUpdateCheck(AdminSysRoleUpsertRequest request) {
        if (SysConstant.SUPER_ADMIN_ROLE_VALUE.equalsIgnoreCase(request.getValue())) {
            // 角色编码不能为SuperAdmin
            throw new BusinessException(SysErrorCodeEnum.A01010, SysConstant.SUPER_ADMIN_ROLE_VALUE);
        }
        if (SysConstant.TENANT_ADMIN_ROLE_VALUE.equalsIgnoreCase(request.getValue()) && !request.creatingNewTenantAdmin()) {
            // 除非是新增租户时，同时新增租户管理员角色，否则角色编码不能为Admin
            throw new BusinessException(SysErrorCodeEnum.A01010, SysConstant.TENANT_ADMIN_ROLE_VALUE);
        }

        boolean isUpdating = Objects.nonNull(request.getId());
        if (isUpdating) {
            SysRoleEntity existingRole = sysRoleMapper.selectById(request.getId());
            SysErrorCodeEnum.A01001.assertNotNull(existingRole);
            if (existingRole.isSuperAdmin() || existingRole.isTenantAdmin()) {
                // 原来角色编码为SuperAdmin或Admin的，不能被改变
                throw new BusinessException(SysErrorCodeEnum.A01010, existingRole.getValue());
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

        List<SysRoleEntity> existingEntityList = sysRoleMapper.selectBatchIds(ids);
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
