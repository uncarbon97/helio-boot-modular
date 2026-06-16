package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.base.util.StreamFunction;
import cc.uncarbon.framework.helium.db.constant.SQLSegment;
import cc.uncarbon.framework.helium.tenant.context.SimpleTenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.module.commons.exception.HasRepeatRecordException;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import cc.uncarbon.module.sys.dal.mapper.SysRoleMapper;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.enums.SysRoleFlagEnum;
import cc.uncarbon.module.sys.helper.UserRoleHelper;
import cc.uncarbon.module.sys.model.internal.UserRoleScope;
import cc.uncarbon.module.sys.model.query.AdminSysRoleListQuery;
import cc.uncarbon.module.sys.model.request.AdminSysRoleBindMenuRequest;
import cc.uncarbon.module.sys.model.request.AdminSysRoleUpsertRequest;
import cc.uncarbon.module.sys.model.request.TenantRoleCreateRequest;
import cc.uncarbon.module.sys.model.response.TenantRoleCreateResult;
import cc.uncarbon.module.sys.model.valueobj.SysRoleDTO;
import cc.uncarbon.module.sys.service.SysMenuService;
import cc.uncarbon.module.sys.service.SysRoleMenuRelationService;
import cc.uncarbon.module.sys.service.SysRoleService;
import cc.uncarbon.module.sys.service.SysUserRoleRelationService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
    private static final Set<String> UNACCEPTABLE_ROLE_CODES = Set.of(
            SysConstant.SUPER_ADMIN_ROLE_CODE, SysConstant.TENANT_ADMIN_ROLE_CODE);

    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleRelationService sysUserRoleRelationService;
    private final SysRoleMenuRelationService sysRoleMenuRelationService;
    private final SysMenuService sysMenuService;
    private final UserRoleHelper userRoleHelper;


    @Override
    public PageResult<SysRoleDTO> adminList(AdminSysRoleListQuery query) {
        Set<Long> invisibleRoleIds = userRoleHelper.listInvisibleRoleIds();
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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long adminCreate(AdminSysRoleUpsertRequest request) {
        log.info(LOG_PREFIX + "新增 >> {}", request);
        checkRepeat(request);
        checkBeforeCreate(request);

        request.setId(null);
        var entity = new SysRoleEntity();
        BeanUtil.copyProperties(request, entity);

        sysRoleMapper.insert(entity);
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminUpdate(AdminSysRoleUpsertRequest request) {
        log.info(LOG_PREFIX + "修改 >> {}", request);
        checkExistence(request.getId());
        checkRepeat(request);
        checkBeforeUpdate(request);

        // 暂不检查该角色是否为当前用户关联的角色

        var entity = new SysRoleEntity();
        BeanUtil.copyProperties(request, entity);

        sysRoleMapper.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminDelete(Collection<Long> ids) {
        log.info(LOG_PREFIX + "删除 >> {}", ids);
        checkBeforeDelete(ids);
        sysRoleMapper.deleteByIds(ids);
    }

    @Override
    public SysRoleDTO getById(Long id) {
        if (id == null) return null;
        var entity = sysRoleMapper.selectById(id);
        return convertEntity(entity, true);
    }

    @Override
    public SysRoleDTO getNonnullById(Long id) throws NoRecordException {
        return NoRecordException.throwIfNull(getById(id));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Set<String> adminBindMenu(AdminSysRoleBindMenuRequest request) {
        checkExistence(request.getRoleId());
        checkBeforeBindRoleMenuRelation(request);
        sysRoleMenuRelationService.cleanAndBind(request.getRoleId(), request.getMenuIds());
        return sysMenuService.listPermissionsByMenus(request.getMenuIds());
    }

    /**
     * 后台管理-下拉框数据
     */
    @Override
    public List<SysRoleDTO> adminSelectOptions() {
        Set<Long> invisibleRoleIds = userRoleHelper.listInvisibleRoleIds();
        List<SysRoleEntity> entityList = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRoleEntity>()
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

    @Override
    public TenantRoleCreateResult createTenantRole(TenantRoleCreateRequest request) {
        try {
            TenantContextHolder.setTenantContext(new SimpleTenantContext(
                    request.getTenantId(), request.getTenantCode(), null));

            var entity = new SysRoleEntity();
            BeanUtil.copyProperties(request, entity);
            // 按需改写字段
            if (request.isTenantAdmin()) {
                entity
                        .setCode(SysConstant.SUPER_ADMIN_ROLE_CODE)
                        .setName("主管理员")
                        .setDescription("具有所有功能权限和全部数据可见范围")
                        .assignFlags(List.of(SysRoleFlagEnum.BUILTIN));
            }

            sysRoleMapper.insert(entity);
            return new TenantRoleCreateResult(entity.getId(), request.isTenantAdmin());
        } finally {
            TenantContextHolder.clear();
        }
    }

    /**
     * 后台管理-删除指定租户的特定角色
     *
     * @param tenantIds 租户IDs，非主键ID，必填
     * @param roleCodes 角色值集合，可以为空
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminDeleteTenantRoles(Collection<Long> tenantIds, Collection<String> roleCodes) {
        if (CollUtil.isEmpty(tenantIds)) {
            return;
        }

        sysRoleMapper.delete(
                new LambdaQueryWrapper<SysRoleEntity>()
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
        List<Long> roleIds = sysUserRoleRelationService.listRoleIdsByUser(userId);

        if (CollUtil.isEmpty(roleIds)) {
            return Map.of();
        }

        // 根据角色Ids取 map
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRoleEntity>()
                .select(SysRoleEntity::getId, SysRoleEntity::getCode)
                .in(SysRoleEntity::getId, roleIds)
        ).stream().collect(Collectors.toMap(SysRoleEntity::getId, SysRoleEntity::getCode, StreamFunction.keepExisting()));
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
        if (entity == null) return null;

        var ret = new SysRoleDTO();
        BeanUtil.copyProperties(entity, ret, "flags");
        // 按需改写字段
        ret.setFlags(entity.resolveFlags());
        if (fillMenu) {
            ret.setMenuIds(sysRoleMenuRelationService.listMenuIdsByRoles(Set.of(ret.getId())));
        }

        return ret;
    }

    /**
     * 实体转值对象
     *
     * @param fillMenu 是否填充菜单
     */
    private List<SysRoleDTO> convertList(List<SysRoleEntity> entityList, boolean fillMenu) {
        if (CollUtil.isEmpty(entityList)) {
            return List.of();
        }
        return entityList.stream().map(item -> convertEntity(item, fillMenu)).toList();
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
        var entity = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRoleEntity>()
                // 仅取主键ID
                .select(SysRoleEntity::getId)
                // 并非原地更新
                .ne(Objects.nonNull(request.getId()), SysRoleEntity::getId, request.getId())
                // 编码相同
                .eq(SysRoleEntity::getCode, request.getCode())
                .last(SQLSegment.LIMIT_1)
        );

        if (entity != null) {
            throw new HasRepeatRecordException("已存在相同的角色编码");
        }
    }

    /**
     * 新增前检查
     */
    private void checkBeforeCreate(AdminSysRoleUpsertRequest request) {
        if (CollUtil.contains(UNACCEPTABLE_ROLE_CODES, request.getCode())) {
            throw new BusinessException(SysErrorCodeEnum.A01010, request.getCode());
        }
    }

    /**
     * 修改前检查
     */
    private void checkBeforeUpdate(AdminSysRoleUpsertRequest request) {
        var entity = sysRoleMapper.selectById(request.getId());
        denyBuiltinOp(entity, SysErrorCodeEnum.A01013);
        if (CollUtil.contains(UNACCEPTABLE_ROLE_CODES, request.getCode())) {
            throw new BusinessException(SysErrorCodeEnum.A01010, request.getCode());
        }
    }

    /**
     * 删除前检查
     */
    private void checkBeforeDelete(Collection<Long> ids) {
        List<SysRoleEntity> entityList = sysRoleMapper.selectByIds(ids);
        for (SysRoleEntity entity : entityList) {
            denyBuiltinOp(entity, SysErrorCodeEnum.A01011);
        }

        UserRoleScope me = userRoleHelper.getCurrentUserRole();
        if (CollUtil.containsAny(me.getRelatedRoleIds(), ids)) {
            throw new BusinessException(SysErrorCodeEnum.A01012);
        }
    }

    /**
     * 绑定角色与菜单关联关系前检查
     * 防止越权访问漏洞
     */
    private void checkBeforeBindRoleMenuRelation(AdminSysRoleBindMenuRequest request) {
        var entity = sysRoleMapper.selectById(request.getRoleId());
        denyBuiltinOp(entity, SysErrorCodeEnum.A01013);

        UserRoleScope me = userRoleHelper.getCurrentUserRole();
        if (CollUtil.contains(me.getRelatedRoleIds(), request.getRoleId())) {
            throw new BusinessException(SysErrorCodeEnum.A01014);
        }

        if (CollUtil.isNotEmpty(request.getMenuIds()) && !me.isSuperAdmin()) {
            // 超级管理员之外的角色，都需要校验自身菜单范围是否满足输入值
            Set<Long> visibleMenuIds = sysRoleMenuRelationService.listMenuIdsByRoles(me.getRelatedRoleIds());
            if (!CollUtil.containsAll(visibleMenuIds, request.getMenuIds())) {
                // 可能存在超自身权限赋权
                throw new BusinessException(SysErrorCodeEnum.A01015);
            }
        }
    }

    /**
     * 阻止对 {@link SysRoleFlagEnum#BUILTIN} 角色的操作
     */
    private static void denyBuiltinOp(SysRoleEntity entity, SysErrorCodeEnum errorCode) {
        if (entity == null) {
            return;
        }
        List<SysRoleFlagEnum> roleFlags = entity.resolveFlags();
        if (CollUtil.contains(roleFlags, SysRoleFlagEnum.BUILTIN)) {
            throw new BusinessException(errorCode);
        }
    }
}
