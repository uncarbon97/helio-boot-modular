package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.framework.helium.db.constant.SQLSegment;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.dal.entity.SysDeptEntity;
import cc.uncarbon.module.sys.dal.mapper.SysDeptMapper;
import cc.uncarbon.module.sys.model.interior.UserDeptContainer;
import cc.uncarbon.module.sys.model.interior.UserRoleContainer;
import cc.uncarbon.module.sys.model.request.AdminSysDeptUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysDeptDTO;
import cc.uncarbon.module.sys.service.SysDeptService;
import cc.uncarbon.module.sys.service.SysRoleService;
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
    private final SysRoleService sysRoleService;


    @Override
    public List<SysDeptDTO> adminList() {
        return convertList(sysDeptMapper.selectSortedList());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long adminCreate(AdminSysDeptUpsertRequest request) {
        log.info(LOG_PREFIX + "新增 >> {}", request);
        checkRepeat(request);

        if (ObjectUtil.isNull(request.getParentId())) {
            request.setParentId(SysConstant.ROOT_PARENT_ID);
        }

        request.setId(null);
        SysDeptEntity entity = new SysDeptEntity();
        BeanUtil.copyProperties(request, entity);

        sysDeptMapper.insert(entity);

        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminUpdate(AdminSysDeptUpsertRequest request) {
        log.info(LOG_PREFIX + "编辑 >> {}", request);
        checkExistence(request.getId());
        checkRepeat(request);

        if (ObjectUtil.isNull(request.getParentId())) {
            request.setParentId(SysConstant.ROOT_PARENT_ID);
        }

        SysDeptEntity entity = new SysDeptEntity();
        BeanUtil.copyProperties(request, entity);

        sysDeptMapper.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminDelete(Collection<Long> ids) {
        log.info(LOG_PREFIX + "删除 >> {}", ids);
        sysDeptMapper.deleteByIds(ids);
    }

    @Override
    public List<SysDeptDTO> adminSelectOptions(boolean inferiorsOnly) {
        if (inferiorsOnly) {
            UserRoleContainer currentUser = sysRoleService.getCurrentUserRoleContainer();
            if (currentUser.isNotAnyAdmin()) {
                // 非管理员才会限制，只能看到本部门及以下
                UserDeptContainer deptContainer = getCurrentUserDeptContainer(true);
                return convertList(deptContainer.getVisibleDepts());
            }
        }
        // 能看所有
        return adminList();
    }

    @Override
    public SysDeptDTO getById(Long id) {
        SysDeptEntity entity = sysDeptMapper.selectById(id);
        return convertEntity(entity);
    }

    @Override
    public SysDeptDTO getNonnullById(Long id) throws NoRecordException {
        return NoRecordException.throwIfNull(getById(id));
    }

    @Override
    public UserDeptContainer getCurrentUserDeptContainer(boolean queryVisibleDept) {
        return getSpecifiedUserDeptContainer(UserContextHolder.getUserId(), queryVisibleDept);
    }

    @Override
    public UserDeptContainer getSpecifiedUserDeptContainer(Long specifiedUserId, boolean queryVisibleDept) {
        List<Long> userDeptIds = sysUserDeptRelationService.listDeptIdsByUser(specifiedUserId);
        List<SysDeptEntity> userDepts = null;
        if (CollUtil.isNotEmpty(userDeptIds)) {
            userDepts = sysDeptMapper.selectByIds(userDeptIds);
        }
        if (CollUtil.isEmpty(userDepts)) {
            userDepts = List.of();
        }
        UserDeptContainer container = new UserDeptContainer(userDeptIds, userDepts);

        if (queryVisibleDept && container.hasRelatedDepts()) {
            List<SysDeptEntity> allDepts = sysDeptMapper.selectSortedList();
            List<SysDeptEntity> inferiors = determineAllInferiors(allDepts, container.primaryRelatedDept());
            container.updateVisibleDepts(inferiors);
        }
        return container;
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
        if (entity == null) {
            return null;
        }

        SysDeptDTO ret = new SysDeptDTO();
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
     * 检查是否存在重复
     */
    private void checkRepeat(AdminSysDeptUpsertRequest request) {
        // ignored
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

        // 循环填充下级部门实例
        while (CollUtil.isNotEmpty(deque)) {
            SysDeptEntity parent = deque.pop();
            List<SysDeptEntity> children = groupByParentId.get(parent.getId());
            if (CollUtil.isNotEmpty(children)) {
                ret.addAll(children);
                deque.addAll(children);
            }
        }
        return ret;
    }
}
