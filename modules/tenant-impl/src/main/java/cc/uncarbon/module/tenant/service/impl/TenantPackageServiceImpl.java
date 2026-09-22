package cc.uncarbon.module.tenant.service.impl;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.commons.constant.SQLSegment;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.commons.model.request.AdminSetStatusRequest;
import cc.uncarbon.module.sys.facade.SysMenuFacade;
import cc.uncarbon.module.sys.facade.TenantUserRoleFacade;
import cc.uncarbon.module.tenant.dal.entity.TenantMetaEntity;
import cc.uncarbon.module.tenant.dal.entity.TenantPackageEntity;
import cc.uncarbon.module.tenant.dal.mapper.TenantMetaMapper;
import cc.uncarbon.module.tenant.dal.mapper.TenantPackageMapper;
import cc.uncarbon.module.tenant.errorcode.TenantErrorCodeEnum;
import cc.uncarbon.module.tenant.model.query.AdminTenantPackageListQuery;
import cc.uncarbon.module.tenant.model.request.AdminTenantPackageBindMenuRequest;
import cc.uncarbon.module.tenant.model.request.AdminTenantPackageUpsertRequest;
import cc.uncarbon.module.tenant.model.valueobj.TenantPackageBindMenuResult;
import cc.uncarbon.module.tenant.model.valueobj.TenantPackageDTO;
import cc.uncarbon.module.tenant.service.TenantPackageMenuRelationService;
import cc.uncarbon.module.tenant.service.TenantPackageService;
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


/**
 * 租户套餐
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class TenantPackageServiceImpl implements TenantPackageService {

    private static final String LOG_PREFIX = "[租户管理][租户套餐]";

    private final TenantPackageMapper tenantPackageMapper;
    private final TenantPackageMenuRelationService tenantPackageMenuRelationService;
    private final TenantMetaMapper tenantMetaMapper;
    private final TenantUserRoleFacade tenantUserRoleFacade;
    private final SysMenuFacade sysMenuFacade;


    @Override
    public PageResult<TenantPackageDTO> adminList(AdminTenantPackageListQuery query) {
        Page<TenantPackageEntity> entityPage = tenantPackageMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<TenantPackageEntity>()
                        // 套餐编码
                        .like(CharSequenceUtil.isNotBlank(query.getCode()), TenantPackageEntity::getCode, CharSequenceUtil.cleanBlank(query.getCode()))
                        // 套餐名称
                        .like(CharSequenceUtil.isNotBlank(query.getName()), TenantPackageEntity::getName, CharSequenceUtil.cleanBlank(query.getName()))
                        // 状态
                        .eq(Objects.nonNull(query.getStatus()), TenantPackageEntity::getStatus, query.getStatus())
                        .orderByDesc(TenantPackageEntity::getId)
        );
        return convertPage(entityPage, false);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long adminCreate(AdminTenantPackageUpsertRequest request) {
        log.info(LOG_PREFIX + "新增 >> {}", request);
        checkRepeat(request);

        request.setId(null);
        var entity = new TenantPackageEntity();
        BeanUtil.copyProperties(request, entity);
        // 新增默认为禁用
        entity.setStatus(EnabledStatusEnum.DISABLED);

        tenantPackageMapper.insert(entity);
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminUpdate(AdminTenantPackageUpsertRequest request) {
        log.info(LOG_PREFIX + "修改 >> {}", request);
        checkExistence(request.getId());
        checkRepeat(request);

        var entity = new TenantPackageEntity();
        BeanUtil.copyProperties(request, entity);

        tenantPackageMapper.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminDelete(Collection<Long> ids) {
        log.info(LOG_PREFIX + "删除 >> {}", ids);
        // 删除前检查：是否仍有租户依赖这些套餐，避免悬空 packageId 导致租户无法编辑
        boolean tenantUsing = tenantMetaMapper.exists(new LambdaQueryWrapper<TenantMetaEntity>()
                .select(TenantMetaEntity::getId)
                .in(TenantMetaEntity::getPackageId, ids)
                .last(SQLSegment.LIMIT_1)
        );
        if (tenantUsing) {
            throw new BusinessException(TenantErrorCodeEnum.A03008);
        }
        tenantPackageMapper.deleteByIds(ids);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminSetStatus(AdminSetStatusRequest<Long, EnabledStatusEnum> request) {
        log.info(LOG_PREFIX + "修改状态 >> {}", request);
        checkExistence(request.getId());

        // 禁用前检查：是否仍有租户依赖当前套餐
        if (EnabledStatusEnum.DISABLED == request.getNewStatus()) {
            boolean tenantUsing = tenantMetaMapper.exists(new LambdaQueryWrapper<TenantMetaEntity>()
                    .select(TenantMetaEntity::getId)
                    .eq(TenantMetaEntity::getPackageId, request.getId())
                    .last(SQLSegment.LIMIT_1)
            );
            if (tenantUsing) {
                throw new BusinessException(TenantErrorCodeEnum.A03003);
            }
        }

        var entity = new TenantPackageEntity()
                .setId(request.getId())
                .setStatus(request.getNewStatus());
        tenantPackageMapper.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public TenantPackageBindMenuResult adminBindMenu(AdminTenantPackageBindMenuRequest request) {
        // 「仅超管可见」菜单及其子孙不允许进入租户套餐，需先在菜单管理中调整为通用可见
        if (CollUtil.isNotEmpty(request.getMenuIds())
                && CollUtil.containsAny(request.getMenuIds(), sysMenuFacade.listSuperAdminOnlySubtreeMenuIds())) {
            throw new BusinessException(TenantErrorCodeEnum.A03009);
        }

        tenantPackageMenuRelationService.cleanAndBind(request.getId(), request.getMenuIds());

        // 查询受影响的租户ID
        List<TenantMetaEntity> affectedTenants = tenantMetaMapper.selectList(new LambdaQueryWrapper<TenantMetaEntity>()
                .select(TenantMetaEntity::getId)
                .eq(TenantMetaEntity::getPackageId, request.getId())
        );

        // 及租户内的所有角色
        Map<Long, Set<Long>> tenantRoleIdsMap = new HashMap<>();
        for (TenantMetaEntity tenant : affectedTenants) {
            tenantRoleIdsMap.put(tenant.getId(),
                    tenantUserRoleFacade.syncTenantRoleMenus(tenant.getId(), request.getMenuIds()));
        }
        return new TenantPackageBindMenuResult(request.getId(), tenantRoleIdsMap);
    }

    @Override
    public List<TenantPackageDTO> adminListSelectOption() {
        List<TenantPackageEntity> entityList = tenantPackageMapper.selectList(new LambdaQueryWrapper<TenantPackageEntity>()
                // 只取特定字段
                .select(TenantPackageEntity::getId, TenantPackageEntity::getCode, TenantPackageEntity::getName)
                // 只显示启用中的套餐
                .eq(TenantPackageEntity::getStatus, EnabledStatusEnum.ENABLED)
                // 排序
                .orderByAsc(TenantPackageEntity::getId)
        );
        return convertList(entityList, false);
    }

    @Override
    public TenantPackageDTO getById(Long id) {
        if (id == null) return null;
        var entity = tenantPackageMapper.selectById(id);
        return convertEntity(entity, true);
    }

    @Override
    public TenantPackageDTO getNonnullById(Long id) throws NoRecordException {
        return NoRecordException.throwIfNull(getById(id));
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
    private TenantPackageDTO convertEntity(TenantPackageEntity entity, boolean fillMenu) {
        if (entity == null) return null;

        var ret = new TenantPackageDTO();
        BeanUtil.copyProperties(entity, ret);
        // 按需改写字段
        if (fillMenu) {
            ret.setMenuIds(tenantPackageMenuRelationService.listMenuIdsByPackage(ret.getId()));
        }
        return ret;
    }

    /**
     * 实体转值对象
     *
     * @param fillMenu 是否填充菜单
     */
    private List<TenantPackageDTO> convertList(List<TenantPackageEntity> entityList, boolean fillMenu) {
        List<TenantPackageDTO> ret = new ArrayList<>(entityList.size());
        for (TenantPackageEntity entity : entityList) {
            ret.add(convertEntity(entity, fillMenu));
        }
        return ret;
    }

    /**
     * 实体转值对象
     *
     * @param fillMenu 是否填充菜单
     */
    private PageResult<TenantPackageDTO> convertPage(Page<TenantPackageEntity> entityPage, boolean fillMenu) {
        return new PageResult<TenantPackageDTO>()
                .setCurrent(entityPage.getCurrent())
                .setSize(entityPage.getSize())
                .setTotal(entityPage.getTotal())
                .setRecords(convertList(entityPage.getRecords(), fillMenu));
    }

    /**
     * 检查是否存在重复
     */
    private void checkRepeat(AdminTenantPackageUpsertRequest request) {
        var entity = tenantPackageMapper.selectOne(new LambdaQueryWrapper<TenantPackageEntity>()
                .select(TenantPackageEntity::getId)
                // 并非原地更新
                .ne(Objects.nonNull(request.getId()), TenantPackageEntity::getId, request.getId())
                // 套餐编码相同
                .eq(TenantPackageEntity::getCode, request.getCode())
                .last(SQLSegment.LIMIT_1)
        );

        if (entity != null) {
            throw new BusinessException(TenantErrorCodeEnum.A03004);
        }
    }

    /**
     * 检查是否存在存在
     */
    private void checkExistence(Long id) {
        boolean exists = tenantPackageMapper.exists(
                new LambdaQueryWrapper<TenantPackageEntity>()
                        .select(TenantPackageEntity::getId)
                        .eq(TenantPackageEntity::getId, id)
                        .last(SQLSegment.LIMIT_1)
        );
        NoRecordException.throwIfFalse(exists);
    }

}
