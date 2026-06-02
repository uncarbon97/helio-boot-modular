package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.framework.helium.base.util.StreamFunction;
import cc.uncarbon.framework.helium.db.constant.SQLSegment;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.commons.exception.HasRepeatRecordException;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.dal.entity.SysMenuEntity;
import cc.uncarbon.module.sys.dal.mapper.SysMenuMapper;
import cc.uncarbon.module.sys.enums.SysErrorCodeEnum;
import cc.uncarbon.module.sys.enums.SysMenuTypeEnum;
import cc.uncarbon.module.sys.model.request.AdminSysMenuUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysMenuBO;
import cc.uncarbon.module.sys.model.valueobj.VbenAdminMenuMetaVO;
import cc.uncarbon.module.sys.service.SysMenuService;
import cc.uncarbon.module.sys.service.SysRoleMenuRelationService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 系统菜单
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class SysMenuServiceImpl implements SysMenuService {

    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuRelationService sysRoleMenuRelationService;

    /**
     * 仅用于输出一个，可按时间流逝而增长的纯数字，避免重复
     */
    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(0L, 0L);


    /**
     * 后台管理-列表
     */
    @Override
    public List<SysMenuBO> adminList() {
        List<SysMenuEntity> entityList = sysMenuMapper.selectList(
                new QueryWrapper<SysMenuEntity>()
                        .lambda()
                        // 排序
                        .orderByAsc(SysMenuEntity::getSort)
        );

        return this.entityList2BOs(entityList);
    }

    /**
     * 根据 ID 取详情
     */
    @Override
    public SysMenuBO getById(Long id) {
        SysMenuEntity entity = sysMenuMapper.selectById(id);
        return this.entity2BO(entity);
    }

    /**
     * 根据 ID 取详情，未取到会抛出 {@link NoRecordException}
     */
    @Override
    public SysMenuBO getNonnullById(Long id) throws NoRecordException {
        return NoRecordException.throwIfNull(getById(id));
    }

    /**
     * 后台管理-新增
     *
     * @return 主键ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long adminCreate(AdminSysMenuUpsertRequest request) {
        log.info("[后台管理-新增系统菜单] >> 入参={}", request);
        checkRepeat(request);

        if (ObjectUtil.isNull(request.getParentId())) {
            request.setParentId(SysConstant.ROOT_PARENT_ID);
        }

        request.setId(null);

        SysMenuEntity entity = new SysMenuEntity();
        BeanUtil.copyProperties(request, entity);

        sysMenuMapper.insert(entity);

        return entity.getId();
    }

    /**
     * 后台管理-修改
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminUpdate(AdminSysMenuUpsertRequest request) {
        log.info("[后台管理-修改系统菜单] >> 入参={}", request);
        checkExistence(request.getId());
        checkRepeat(request);

        if (ObjectUtil.isNull(request.getParentId())) {
            request.setParentId(SysConstant.ROOT_PARENT_ID);
        }

        SysMenuEntity entity = new SysMenuEntity();
        BeanUtil.copyProperties(request, entity);

        sysMenuMapper.updateById(entity);
    }

    /**
     * 后台管理-删除
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminDelete(Collection<Long> ids) {
        log.info("[后台管理-删除系统菜单] >> 入参={}", ids);
        sysMenuMapper.deleteByIds(ids);
    }

    /**
     * 后台管理-取侧边菜单
     */
    @Override
    public List<SysMenuBO> adminListSideMenu() {
        Set<Long> visibleMenuIds = listCurrentUserVisibleMenuIds();
        return this.listByIds(visibleMenuIds, SysMenuTypeEnum.forAdminSide());
    }

    /**
     * 后台管理-取所有可见菜单
     */
    @Override
    public List<SysMenuBO> adminListVisibleMenu() {
        Set<Long> visibleMenuIds = listCurrentUserVisibleMenuIds();
        return this.listByIds(visibleMenuIds, SysMenuTypeEnum.all());
    }

    /**
     * 根据角色Ids，获取角色ID 对应的权限名 Map
     *
     * @return map key=角色ID value=权限名集合
     */
    @Override
    public Map<Long, Set<String>> getRoleIdPermissionMap(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return Map.of();
        }

        Map<Long, Set<String>> ret = new HashMap<>(roleIds.size(), 1);

        roleIds.forEach(
                roleId -> {
                    Set<String> permissions;

                    if (SysConstant.SUPER_ADMIN_ROLE_ID.equals(roleId)) {
                        // 超级管理员读取所有权限，不管有没有被禁用
                        permissions = sysMenuMapper.selectList(null).stream()
                                .map(SysMenuEntity::getPermission)
                                .filter(CharSequenceUtil::isNotEmpty)
                                .collect(Collectors.toSet());
                    } else {
                        // 非超级管理员则通过角色ID，关联查询拥有的菜单，菜单上有权限名

                        Set<Long> menuIds = sysRoleMenuRelationService.listMenuIdsByRoleIds(Collections.singleton(roleId));
                        if (CollUtil.isEmpty(menuIds)) {
                            permissions = Collections.emptySet();
                        } else {
                            permissions = sysMenuMapper.selectList(
                                            new QueryWrapper<SysMenuEntity>()
                                                    .lambda()
                                                    .select(SysMenuEntity::getPermission)
                                                    .in(SysMenuEntity::getId, menuIds)
                                                    .eq(SysMenuEntity::getStatus, EnabledStatusEnum.ENABLED)
                                    )
                                    .stream()
                                    .map(SysMenuEntity::getPermission)
                                    .filter(StrUtil::isNotEmpty)
                                    .collect(Collectors.toSet());
                        }
                    }

                    ret.put(roleId, permissions);
                }
        );

        return ret;
    }

    /**
     * 根据菜单ID集合，取权限名集合
     */
    @Override
    public Set<String> listPermissionsByMenuIds(Collection<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return Collections.emptySet();
        }

        return sysMenuMapper.selectList(
                new QueryWrapper<SysMenuEntity>()
                        .lambda()
                        .select(SysMenuEntity::getPermission)
                        .in(SysMenuEntity::getId, menuIds)
        ).stream().map(SysMenuEntity::getPermission).filter(StrUtil::isNotEmpty).collect(Collectors.toSet());
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    /**
     * 实体转值对象
     */
    private SysMenuBO entity2BO(SysMenuEntity entity) {
        if (entity == null) {
            return null;
        }

        SysMenuBO bo = new SysMenuBO();
        BeanUtil.copyProperties(entity, bo);

        // 按需改写字段
        if (SysConstant.ROOT_PARENT_ID.equals(bo.getParentId())) {
            bo.setParentId(null);
        }

        String snowflakeIdStr = SNOWFLAKE.nextIdStr();
        bo
                .setName(bo.getName())
                .setMeta(new VbenAdminMenuMetaVO(bo.getName(), false, bo.getIcon()));

        switch (bo.getMenuType()) {
            case DIR, BUTTON -> bo
                    .setComponent(SysConstant.VBEN_ADMIN_BLANK_VIEW)
                    .setExternalLink(null)
                    .setPath(StrPool.SLASH + snowflakeIdStr);
            case MENU -> {
                bo
                        .setExternalLink(null)
                        .setPath(bo.getComponent());
                // 防止用户忘记加了, 主动补充/
                if (CharSequenceUtil.isNotBlank(bo.getPath()) && !bo.getPath().startsWith(StrPool.SLASH)) {
                    bo.setPath(StrPool.SLASH + bo.getPath());
                }
            }
            case EXTERNAL_LINK -> bo
                    .setComponent(bo.getExternalLink())
                    .setPath(bo.getExternalLink());
        }
        return bo;
    }

    private List<SysMenuBO> entityList2BOs(List<SysMenuEntity> entityList) {
        // 深拷贝
        List<SysMenuBO> ret = new ArrayList<>(entityList.size());
        entityList.forEach(
                entity -> ret.add(this.entity2BO(entity))
        );

        return ret;
    }

    /**
     * 取当前账号可见菜单Ids
     *
     * @return 菜单Ids
     */
    private Set<Long> listCurrentUserVisibleMenuIds() {
        // 1. 取当前账号拥有角色Ids
        var roleIds = UserContextHolder.getUserContext().getRolesIds();
        SysErrorCodeEnum.A01005.assertNotEmpty(roleIds);

        // 2. 得到所有可用的 菜单ID-上级菜单ID map，备用
        Map<Long, Long> allMenuMap = sysMenuMapper.selectList(
                new QueryWrapper<SysMenuEntity>()
                        .lambda()
                        .select(SysMenuEntity::getId, SysMenuEntity::getParentId)
                        .eq(SysMenuEntity::getStatus, EnabledStatusEnum.ENABLED)
        ).stream().collect(Collectors.toMap(SysMenuEntity::getId, SysMenuEntity::getParentId, StreamFunction.ignoredThrowingMerger()));

        // 3. 超级管理员直接返回所有菜单
        if (roleIds.contains(SysConstant.SUPER_ADMIN_ROLE_ID)) {
            return new HashSet<>(allMenuMap.keySet());
        }

        // 4. 根据现有角色，获取直接关联的菜单ID
        Set<Long> directlyRelatedMenuIds = sysRoleMenuRelationService.listMenuIdsByRoleIds(roleIds);
        SysErrorCodeEnum.A01006.assertNotEmpty(directlyRelatedMenuIds);

        // 5. 因为直接关联的菜单ID，可能不包含父级菜单，使得级联关系缺失，这里得给他补上
        return this.traceParentMenuIds(allMenuMap, directlyRelatedMenuIds);
    }

    private List<SysMenuBO> listByIds(Collection<Long> ids, List<SysMenuTypeEnum> types)
            throws IllegalArgumentException {
        Assert.notEmpty(ids);
        Assert.notEmpty(types);

        List<SysMenuEntity> entityList = sysMenuMapper.selectList(
                new LambdaQueryWrapper<SysMenuEntity>()
                        .in(SysMenuEntity::getId, ids)
                        .in(SysMenuEntity::getMenuType, types)
                        // 仅显示启用状态菜单
                        .eq(SysMenuEntity::getStatus, EnabledStatusEnum.ENABLED)
                        .orderByAsc(SysMenuEntity::getSort)
        );

        if (CollUtil.isEmpty(entityList)) {
            return List.of();
        }

        return this.entityList2BOs(entityList);
    }

    /**
     * 检查是否存在
     */
    private void checkExistence(Long id) {
        boolean exists = sysMenuMapper.exists(
                new LambdaQueryWrapper<SysMenuEntity>()
                        .select(SysMenuEntity::getId)
                        .eq(SysMenuEntity::getId, id)
                        .last(SQLSegment.LIMIT_1)
        );
        NoRecordException.throwIfFalse(exists);
    }

    /**
     * 检查是否存在重复
     */
    private void checkRepeat(AdminSysMenuUpsertRequest request) {
        if (CharSequenceUtil.isNotBlank(request.getPermission())) {
            request.setPermission(CharSequenceUtil.cleanBlank(request.getPermission()));

            SysMenuEntity entity = sysMenuMapper.selectOne(
                    new LambdaQueryWrapper<SysMenuEntity>()
                            .select(SysMenuEntity::getId)
                            // 并非原地更新
                            .ne(Objects.nonNull(request.getId()), SysMenuEntity::getId, request.getId())
                            // 权限标识相同
                            .eq(SysMenuEntity::getPermission, request.getPermission())
                            .last(SQLSegment.LIMIT_1)
            );

            if (entity != null) {
                throw new HasRepeatRecordException("已存在相同的【权限标识】");
            }
        }
    }

    /**
     * 追溯并补充可能缺失的级联上级菜单ID
     *
     * @param allMenuMap             完整的 菜单ID-上级菜单ID map
     * @param directlyRelatedMenuIds 当前用户直接关联的菜单ID集合
     * @return Set<Long> 已经补充好的、完整的菜单ID集合
     */
    private Set<Long> traceParentMenuIds(Map<Long, Long> allMenuMap, Collection<Long> directlyRelatedMenuIds) {
        // 返回值
        Set<Long> ret = new HashSet<>(directlyRelatedMenuIds.size() << 1);
        ret.addAll(directlyRelatedMenuIds);

        // 本次循环要遍历的菜单ID集合
        HashSet<Long> thisLoop;
        // 下次循环要遍历的菜单ID集合
        HashSet<Long> nextLoop = new HashSet<>(directlyRelatedMenuIds);

        do {
            // 深拷贝数据，用于本次循环
            thisLoop = new HashSet<>(nextLoop);
            nextLoop.clear();

            for (Long menuId : thisLoop) {
                Long parentId = allMenuMap.get(menuId);

                if (Objects.nonNull(parentId)) {
                    // 上级菜单可能还会有祖级菜单，需要继续上溯
                    nextLoop.add(parentId);
                }
            }

            ret.addAll(nextLoop);

        } while (!nextLoop.isEmpty());

        return ret;
    }
}
