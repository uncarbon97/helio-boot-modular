package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.framework.helium.base.util.StreamFunction;
import cc.uncarbon.framework.helium.db.constant.SQLSegment;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.commons.exception.HasRepeatRecordException;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.dal.entity.SysMenuEntity;
import cc.uncarbon.module.sys.dal.mapper.SysMenuMapper;
import cc.uncarbon.module.sys.enums.MenuTypeEnum;
import cc.uncarbon.module.sys.enums.SysErrorCodeEnum;
import cc.uncarbon.module.sys.model.request.AdminSysMenuUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysMenuDTO;
import cc.uncarbon.module.sys.model.valueobj.VbenAdminMenuMetaVO;
import cc.uncarbon.module.sys.service.SysMenuService;
import cc.uncarbon.module.sys.service.SysRoleMenuRelationService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    private static final String LOG_PREFIX = "[系统管理][菜单]";

    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuRelationService sysRoleMenuRelationService;

    /**
     * 仅用于输出一个，可按时间流逝而增长的纯数字，避免重复
     */
    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(0L, 0L);


    @Override
    public List<SysMenuDTO> adminList() {
        List<SysMenuEntity> entityList = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenuEntity>()
                // 排序
                .orderByAsc(SysMenuEntity::getSort)
        );
        return convertList(entityList);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long adminCreate(AdminSysMenuUpsertRequest request) {
        log.info(LOG_PREFIX + "新增 >> {}", request);
        checkRepeat(request);

        if (ObjectUtil.isNull(request.getParentId())) {
            request.setParentId(SysConstant.ROOT_PARENT_ID);
        }

        request.setId(null);

        var entity = new SysMenuEntity();
        BeanUtil.copyProperties(request, entity);

        sysMenuMapper.insert(entity);
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminUpdate(AdminSysMenuUpsertRequest request) {
        log.info(LOG_PREFIX + "修改 >> {}", request);
        checkExistence(request.getId());
        checkRepeat(request);

        if (ObjectUtil.isNull(request.getParentId())) {
            request.setParentId(SysConstant.ROOT_PARENT_ID);
        }

        var entity = new SysMenuEntity();
        BeanUtil.copyProperties(request, entity);

        sysMenuMapper.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminDelete(Collection<Long> ids) {
        log.info(LOG_PREFIX + "删除 >> {}", ids);
        sysMenuMapper.deleteByIds(ids);
    }

    @Override
    public SysMenuDTO getById(Long id) {
        if (id == null) return null;
        var entity = sysMenuMapper.selectById(id);
        return convertEntity(entity);
    }

    @Override
    public SysMenuDTO getNonnullById(Long id) throws NoRecordException {
        return NoRecordException.throwIfNull(getById(id));
    }

    @Override
    public List<SysMenuDTO> adminListSideMenus() {
        Set<Long> visibleMenuIds = listCurrentUserVisibleMenuIds();
        return listByIds(visibleMenuIds, MenuTypeEnum.forAdminSide());
    }

    @Override
    public List<SysMenuDTO> adminListAvailableMenus() {
        Set<Long> visibleMenuIds = listCurrentUserVisibleMenuIds();
        return listByIds(visibleMenuIds, MenuTypeEnum.all());
    }

    @Override
    public Map<Long, Set<String>> getPermissionsByRole(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return Map.of();
        }
        Map<Long, Set<String>> ret = new HashMap<>(roleIds.size(), 1);
        for (Long roleId : roleIds) {
            Set<String> permissions;

            if (SysConstant.SUPER_ADMIN_ROLE_ID.equals(roleId)) {
                // 超级管理员读取所有权限，不管有没有被禁用
                permissions = sysMenuMapper.selectList(null).stream()
                        .map(SysMenuEntity::getPermission)
                        .filter(CharSequenceUtil::isNotEmpty)
                        .collect(Collectors.toSet());
            } else {
                // 查询角色关联菜单
                Set<Long> menuIds = sysRoleMenuRelationService.listMenuIdsByRoles(Set.of(roleId));
                if (CollUtil.isEmpty(menuIds)) {
                    permissions = Set.of();
                } else {
                    permissions = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenuEntity>()
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
        return ret;
    }

    @Override
    public Set<String> listPermissionsByMenus(Collection<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return Set.of();
        }

        return sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenuEntity>()
                .select(SysMenuEntity::getPermission)
                .in(SysMenuEntity::getId, menuIds)
        ).stream().map(SysMenuEntity::getPermission).filter(CharSequenceUtil::isNotEmpty).collect(Collectors.toSet());
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    /**
     * 实体转值对象
     */
    private SysMenuDTO convertEntity(SysMenuEntity entity) {
        if (entity == null) return null;

        var ret = new SysMenuDTO();
        BeanUtil.copyProperties(entity, ret);
        // 按需改写字段
        if (SysConstant.ROOT_PARENT_ID.equals(ret.getParentId())) {
            ret.setParentId(null);
        }

        String snowflakeIdStr = SNOWFLAKE.nextIdStr();
        ret
                .setName(ret.getName())
                .setMeta(new VbenAdminMenuMetaVO(ret.getName(), false, ret.getIcon()));

        switch (ret.getMenuType()) {
            case DIR, BUTTON -> ret
                    .setComponent(SysConstant.VBEN_ADMIN_BLANK_VIEW)
                    .setExternalLink(null)
                    .setPath(StrPool.SLASH + snowflakeIdStr);
            case MENU -> {
                ret
                        .setExternalLink(null)
                        .setPath(ret.getComponent());
                // 防止用户忘记加了, 主动补充/
                if (CharSequenceUtil.isNotBlank(ret.getPath()) && !ret.getPath().startsWith(StrPool.SLASH)) {
                    ret.setPath(StrPool.SLASH + ret.getPath());
                }
            }
            case EXTERNAL_LINK -> ret
                    .setComponent(ret.getExternalLink())
                    .setPath(ret.getExternalLink());
        }
        return ret;
    }

    /**
     * 实体转值对象
     */
    private List<SysMenuDTO> convertList(List<SysMenuEntity> entityList) {
        if (CollUtil.isEmpty(entityList)) {
            return List.of();
        }
        return entityList.stream().map(this::convertEntity).toList();
    }

    /**
     * 取当前账号可见菜单Ids
     *
     * @return 菜单Ids
     */
    private Set<Long> listCurrentUserVisibleMenuIds() {
        assert UserContextHolder.getContext() != null;
        // 1. 取当前账号拥有角色Ids
        var roleIds = UserContextHolder.getContext().getRoleIds();
        SysErrorCodeEnum.A01005.throwIfEmpty(roleIds);

        // 2. 得到所有可用的 菜单ID-上级菜单ID map，备用
        Map<Long, Long> parentIdById = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenuEntity>()
                .select(SysMenuEntity::getId, SysMenuEntity::getParentId)
                .eq(SysMenuEntity::getStatus, EnabledStatusEnum.ENABLED)
        ).stream().collect(Collectors.toMap(SysMenuEntity::getId, SysMenuEntity::getParentId, StreamFunction.keepExisting()));

        // 3. 超级管理员直接返回所有菜单
        if (roleIds.contains(SysConstant.SUPER_ADMIN_ROLE_ID)) {
            return new HashSet<>(parentIdById.keySet());
        }

        // 4. 根据现有角色，获取直接关联的菜单ID
        Set<Long> directlyRelatedMenuIds = sysRoleMenuRelationService.listMenuIdsByRoles(roleIds);
        SysErrorCodeEnum.A01006.throwIfEmpty(directlyRelatedMenuIds);

        // 5. 因为直接关联的菜单ID，可能不包含父级菜单，使得级联关系缺失，这里得给他补上
        return traceParentMenuIds(parentIdById, directlyRelatedMenuIds);
    }

    private List<SysMenuDTO> listByIds(Collection<Long> ids, List<MenuTypeEnum> menuTypes) {
        if (CollUtil.isEmpty(ids)) {
            return List.of();
        }

        List<SysMenuEntity> entityList = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenuEntity>()
                .in(SysMenuEntity::getId, ids)
                .in(SysMenuEntity::getMenuType, menuTypes)
                // 仅显示启用状态菜单
                .eq(SysMenuEntity::getStatus, EnabledStatusEnum.ENABLED)
                .orderByAsc(SysMenuEntity::getSort)
        );
        return convertList(entityList);
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

            var entity = sysMenuMapper.selectOne(new LambdaQueryWrapper<SysMenuEntity>()
                    .select(SysMenuEntity::getId)
                    // 并非原地更新
                    .ne(Objects.nonNull(request.getId()), SysMenuEntity::getId, request.getId())
                    // 权限标识相同
                    .eq(SysMenuEntity::getPermission, request.getPermission())
                    .last(SQLSegment.LIMIT_1)
            );

            if (entity != null) {
                throw new HasRepeatRecordException("已存在相同的权限标识");
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
