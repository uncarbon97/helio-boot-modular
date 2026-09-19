package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.commons.constant.SQLSegment;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.commons.model.request.AdminSetStatusRequest;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.dal.entity.SysDeptEntity;
import cc.uncarbon.module.sys.dal.mapper.SysDeptMapper;
import cc.uncarbon.module.sys.helper.UserRoleHelper;
import cc.uncarbon.module.sys.model.internal.UserDeptScope;
import cc.uncarbon.module.sys.model.internal.UserRoleScope;
import cc.uncarbon.module.sys.model.request.AdminSysDeptUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysDeptDTO;
import cc.uncarbon.module.sys.service.SysDeptService;
import cc.uncarbon.module.sys.service.SysUserDeptRelationService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 部门
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class SysDeptServiceImpl implements SysDeptService {

    private static final String LOG_PREFIX = "[系统管理][部门]";

    private final SysDeptMapper sysDeptMapper;
    private final SysUserDeptRelationService sysUserDeptRelationService;
    private final UserRoleHelper userRoleHelper;


    @Override
    public List<SysDeptDTO> adminList() {
        return convertList(sysDeptMapper.selectSortedList());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long adminCreate(AdminSysDeptUpsertRequest request) {
        log.info(LOG_PREFIX + "新增 >> {}", request);
        defaultParentId(request);
        checkRepeat(request);
        checkParentUsable(request.getParentId());

        request.setId(null);
        var entity = new SysDeptEntity();
        BeanUtil.copyProperties(request, entity);
        if (entity.getStatus() == null) {
            // 新建部门默认启用
            entity.setStatus(EnabledStatusEnum.ENABLED);
        }

        sysDeptMapper.insert(entity);
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminUpdate(AdminSysDeptUpsertRequest request) {
        log.info(LOG_PREFIX + "编辑 >> {}", request);
        checkExistence(request.getId());
        defaultParentId(request);
        checkRepeat(request);
        checkParentUsable(request.getParentId());
        checkParentNotSelfOrInferior(request.getId(), request.getParentId());

        var entity = new SysDeptEntity();
        BeanUtil.copyProperties(request, entity);

        sysDeptMapper.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminDelete(Collection<Long> ids) {
        log.info(LOG_PREFIX + "删除 >> {}", ids);
        checkBeforeDelete(ids);
        // 解除关联关系
        ids.forEach(sysUserDeptRelationService::cleanAllBindings);
        sysDeptMapper.deleteByIds(ids);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminSetStatus(AdminSetStatusRequest<Long, EnabledStatusEnum> request) {
        log.info(LOG_PREFIX + "修改状态 >> {}", request);
        Long id = request.getId();
        var entity = sysDeptMapper.selectById(id);
        NoRecordException.throwIfNull(entity);
        if (request.getNewStatus() == entity.getStatus()) {
            // 状态未变化，幂等返回
            return;
        }
        checkBeforeSetStatus(entity, request.getNewStatus());

        SysDeptEntity template = new SysDeptEntity()
                .setId(id)
                .setStatus(request.getNewStatus());
        sysDeptMapper.updateById(template);
    }

    @Override
    public List<SysDeptDTO> adminListSelectOption(boolean inferiorsOnly) {
        if (inferiorsOnly) {
            UserRoleScope me = userRoleHelper.getCurrentUserRole();
            if (me.isNotAnyAdmin()) {
                // 非管理员才会限制，只能看到本部门及以下
                UserDeptScope deptContainer = getCurrentUserDept(true);
                return convertEnabledList(deptContainer.getVisibleDepts());
            }
        }
        // 能看所有；下拉框只列启用部门，屏蔽对停用部门的新增引用
        return convertEnabledList(sysDeptMapper.selectEnabledSortedList());
    }

    @Override
    public SysDeptDTO getById(Long id) {
        if (id == null) return null;
        var entity = sysDeptMapper.selectById(id);
        return convertEntity(entity);
    }

    @Override
    public SysDeptDTO getNonnullById(Long id) throws NoRecordException {
        return NoRecordException.throwIfNull(getById(id));
    }

    @Override
    public UserDeptScope getCurrentUserDept(boolean queryVisibleDept) {
        return getSpecifiedUserDept(UserContextHolder.getUserId(), queryVisibleDept);
    }

    @Override
    public UserDeptScope getSpecifiedUserDept(Long specifiedUserId, boolean queryVisibleDept) {
        List<Long> userDeptIds = sysUserDeptRelationService.listDeptIdsByUser(specifiedUserId);
        List<SysDeptEntity> userDepts = null;
        if (CollUtil.isNotEmpty(userDeptIds)) {
            userDepts = sysDeptMapper.selectByIds(userDeptIds);
        }
        if (CollUtil.isEmpty(userDepts)) {
            userDepts = List.of();
        }
        UserDeptScope scope = new UserDeptScope(userDeptIds, userDepts);

        if (queryVisibleDept && scope.hasRelatedDepts()) {
            List<SysDeptEntity> allDepts = sysDeptMapper.selectSortedList();
            List<SysDeptEntity> inferiors = determineAllInferiors(allDepts, scope.primaryRelatedDept());
            scope.updateVisibleDepts(inferiors);
        }
        return scope;
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    /**
     * 实体转值对象
     */
    private SysDeptDTO convertEntity(SysDeptEntity entity) {
        if (entity == null) return null;

        var ret = new SysDeptDTO();
        BeanUtil.copyProperties(entity, ret);
        // 按需改写字段
        if (SysConstant.ROOT_PARENT_ID.equals(ret.getParentId())) {
            // 返回前端时，不显示 parentId = 0
            ret.setParentId(null);
        }

        return ret;
    }

    /**
     * 实体转值对象
     */
    private List<SysDeptDTO> convertList(List<SysDeptEntity> entityList) {
        if (CollUtil.isEmpty(entityList)) {
            return List.of();
        }
        return entityList.stream().map(this::convertEntity).toList();
    }

    /**
     * 检查是否存在
     */
    private void checkExistence(Long id) {
        boolean exists = sysDeptMapper.exists(
                new LambdaQueryWrapper<SysDeptEntity>()
                        .select(SysDeptEntity::getId)
                        .eq(SysDeptEntity::getId, id)
                        .last(SQLSegment.LIMIT_1)
        );
        NoRecordException.throwIfFalse(exists);
    }

    /**
     * 检查是否存在重复：同一上级部门下不允许同名部门
     */
    private void checkRepeat(AdminSysDeptUpsertRequest request) {
        boolean exists = sysDeptMapper.exists(
                new LambdaQueryWrapper<SysDeptEntity>()
                        .eq(SysDeptEntity::getName, request.getName())
                        .eq(SysDeptEntity::getParentId, request.getParentId())
                        // 并非原地更新
                        .ne(request.getId() != null, SysDeptEntity::getId, request.getId())
                        .last(SQLSegment.LIMIT_1)
        );
        if (exists) {
            throw new BusinessException(SysErrorCodeEnum.A01040);
        }
    }

    /**
     * 删除前检查：有下级部门或关联用户时不能删除，只能停用
     */
    private void checkBeforeDelete(Collection<Long> ids) {
        boolean hasChildren = sysDeptMapper.exists(
                new LambdaQueryWrapper<SysDeptEntity>()
                        .in(SysDeptEntity::getParentId, ids)
        );
        if (hasChildren || CollUtil.isNotEmpty(sysUserDeptRelationService.listUserIdsByDepts(ids))) {
            throw new BusinessException(SysErrorCodeEnum.A01038);
        }
    }

    /**
     * 修改状态前检查，保证状态从叶到根一致
     */
    private void checkBeforeSetStatus(SysDeptEntity entity, EnabledStatusEnum newStatus) {
        if (EnabledStatusEnum.DISABLED == newStatus) {
            // 停用时，存在未停用的下级部门则拒绝
            boolean hasEnabledChildren = sysDeptMapper.exists(
                    new LambdaQueryWrapper<SysDeptEntity>()
                            .eq(SysDeptEntity::getParentId, entity.getId())
                            .eq(SysDeptEntity::getStatus, EnabledStatusEnum.ENABLED)
            );
            if (hasEnabledChildren) {
                throw new BusinessException(SysErrorCodeEnum.A01036);
            }
        } else {
            // 启用时，上级部门已停用则拒绝
            Long parentId = entity.getParentId();
            if (parentId != null && !SysConstant.ROOT_PARENT_ID.equals(parentId)) {
                SysDeptEntity parent = sysDeptMapper.selectById(parentId);
                if (parent == null || EnabledStatusEnum.DISABLED == parent.getStatus()) {
                    throw new BusinessException(SysErrorCodeEnum.A01037);
                }
            }
        }
    }

    /**
     * parentId 为空时，归为根部门
     */
    private static void defaultParentId(AdminSysDeptUpsertRequest request) {
        if (ObjectUtil.isNull(request.getParentId())) {
            request.setParentId(SysConstant.ROOT_PARENT_ID);
        }
    }

    /**
     * 检查新的上级部门不能是自身，也不能是自己的下级部门（避免成环）
     */
    private void checkParentNotSelfOrInferior(Long id, Long parentId) {
        if (id == null || parentId == null || SysConstant.ROOT_PARENT_ID.equals(parentId)) {
            return;
        }
        // 沿新上级的父链向上找，一旦遇到自身即成环
        Long cursor = parentId;
        int guard = 0;
        while (cursor != null && !SysConstant.ROOT_PARENT_ID.equals(cursor)) {
            if (cursor.equals(id)) {
                throw new BusinessException(SysErrorCodeEnum.A01039);
            }
            SysDeptEntity node = sysDeptMapper.selectById(cursor);
            if (node == null) {
                break;
            }
            cursor = node.getParentId();
            if (++guard > 100) {
                // 历史脏数据疑似成环：宁可拒绝也不放行
                throw new BusinessException(SysErrorCodeEnum.A01039);
            }
        }
    }

    /**
     * 检查上级部门是否可用（存在且启用）
     */
    private void checkParentUsable(Long parentId) {
        if (parentId == null || SysConstant.ROOT_PARENT_ID.equals(parentId)) {
            return;
        }
        SysDeptEntity parent = sysDeptMapper.selectById(parentId);
        NoRecordException.throwIfNull(parent);
        if (EnabledStatusEnum.DISABLED == parent.getStatus()) {
            throw new BusinessException(SysErrorCodeEnum.A01037);
        }
    }

    /**
     * 实体转值对象（仅保留启用状态的部门）
     */
    private List<SysDeptDTO> convertEnabledList(List<SysDeptEntity> entityList) {
        if (CollUtil.isEmpty(entityList)) {
            return List.of();
        }
        return entityList.stream()
                .filter(dept -> EnabledStatusEnum.ENABLED == dept.getStatus())
                .map(this::convertEntity)
                .toList();
    }

    /**
     * 找出本部门及所有下级部门
     *
     * @param start 本部门
     * @return 本部门 + 所有下级部门
     */
    private List<SysDeptEntity> determineAllInferiors(List<SysDeptEntity> entityList, SysDeptEntity start) {
        // 结果集合
        List<SysDeptEntity> ret = new ArrayList<>(entityList.size());
        Deque<SysDeptEntity> deque = new ArrayDeque<>();

        // 转map提高效率
        Map<Long, List<SysDeptEntity>> groupByParentId = entityList.stream().collect(Collectors.groupingBy(SysDeptEntity::getParentId));

        // 起点
        ret.add(start);
        deque.add(start);
        // 已遍历部门ID，防止脏数据 parentId 成环导致死循环
        Set<Long> visitedIds = new HashSet<>();
        visitedIds.add(start.getId());

        // 循环填充下级部门实例
        while (CollUtil.isNotEmpty(deque)) {
            SysDeptEntity parent = deque.pop();
            List<SysDeptEntity> children = groupByParentId.get(parent.getId());
            if (CollUtil.isNotEmpty(children)) {
                for (SysDeptEntity child : children) {
                    if (visitedIds.add(child.getId())) {
                        ret.add(child);
                        deque.add(child);
                    }
                }
            }
        }
        return ret;
    }
}
