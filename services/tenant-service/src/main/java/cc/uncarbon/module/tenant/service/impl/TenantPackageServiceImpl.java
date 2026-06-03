package cc.uncarbon.module.tenant.service.impl;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.db.constant.SQLSegment;
import cc.uncarbon.module.commons.enums.DefaultErrorCodeEnum;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.tenant.dal.entity.TenantPackageEntity;
import cc.uncarbon.module.tenant.dal.mapper.TenantPackageMapper;
import cc.uncarbon.module.tenant.model.query.AdminTenantPackageListQuery;
import cc.uncarbon.module.tenant.model.request.AdminBindPackageMenuRelationDTO;
import cc.uncarbon.module.tenant.model.request.AdminTenantPackageUpsertRequest;
import cc.uncarbon.module.tenant.model.valueobj.TenantPackageDTO;
import cc.uncarbon.module.tenant.service.TenantPackageMenuRelationService;
import cc.uncarbon.module.tenant.service.TenantPackageService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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


    @Override
    public PageResult<TenantPackageDTO> adminList(AdminTenantPackageListQuery query) {
        Page<TenantPackageEntity> entityPage = tenantPackageMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new QueryWrapper<TenantPackageEntity>()
                        .lambda()
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long adminCreate(AdminTenantPackageUpsertRequest request) {
        log.info(LOG_PREFIX + "新增 >> {}", request);
        checkRepeat(request);

        request.setId(null);
        TenantPackageEntity entity = new TenantPackageEntity();
        BeanUtil.copyProperties(request, entity);

        tenantPackageMapper.insert(entity);

        // 更新套餐-菜单关联关系
        tenantPackageMenuRelationService.cleanAndBind(entity.getId(), request.getMenuIds());

        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminUpdate(AdminTenantPackageUpsertRequest request) {
        log.info(LOG_PREFIX + "修改 >> {}", request);
        checkExistence(request.getId());
        checkRepeat(request);

        TenantPackageEntity entity = new TenantPackageEntity();
        BeanUtil.copyProperties(request, entity);

        tenantPackageMapper.updateById(entity);

        // 更新套餐-菜单关联关系
        tenantPackageMenuRelationService.cleanAndBind(entity.getId(), request.getMenuIds());

        // TODO 更新现有租户套餐
//        @Override
//        @Transactional(rollbackFor = Exception.class)
//        public void updateTenantMenu(List<Long> newMenuIds, Long packageId) {
//            List<Long> tenantIdList = this.listIdByPackageId(packageId);
//            if (CollUtil.isEmpty(tenantIdList)) {
//                return;
//            }
//            // 所有租户角色：删除旧菜单
//            tenantIdList.forEach(tenantId -> TenantUtils.execute(tenantId, () -> {
//                // 删除旧菜单
//                roleMenuApi.deleteByNotInMenuIds(newMenuIds);
//                // 更新在线用户上下文
//                Set<Long> roleIdSet = roleMenuApi.listRoleIdByNotInMenuIds(newMenuIds);
//                roleIdSet.forEach(roleApi::updateUserContext);
//            }));
//            // 租户管理员：新增菜单
//            tenantIdList.forEach(tenantId -> TenantUtils.execute(tenantId, () -> {
//                Long roleId = roleApi.getIdByCode(RoleCodeEnum.TENANT_ADMIN.getCode());
//                roleMenuApi.add(newMenuIds, roleId);
//                // 更新在线用户上下文
//                roleApi.updateUserContext(roleId);
//            }));
//            // 删除缓存
//            RedisUtils.deleteByPattern(CacheConstants.ROLE_MENU_KEY_PREFIX + StringConstants.ASTERISK);
//        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDelete(Collection<Long> ids) {
        log.info(LOG_PREFIX + "删除 >> {}", ids);
        tenantPackageMapper.deleteByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Set<String> adminBindMenus(AdminBindPackageMenuRelationDTO dto) {
        tenantPackageMenuRelationService.cleanAndBind(dto.getPackageId(), dto.getMenuIds());
        return Set.of();
    }

    @Override
    public TenantPackageDTO getById(Long id) {
        TenantPackageEntity entity = tenantPackageMapper.selectById(id);
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
     */
    private TenantPackageDTO convertEntity(TenantPackageEntity entity, boolean fillMenu) {
        if (entity == null) {
            return null;
        }

        TenantPackageDTO dto = new TenantPackageDTO();
        BeanUtil.copyProperties(entity, dto);

        if (fillMenu) {
            dto.setMenuIds(
                    tenantPackageMenuRelationService.listMenuIdsByPackageId(Collections.singleton(dto.getId()))
            );
        }
        return dto;
    }

    /**
     * 实体转值对象
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
        TenantPackageEntity entity = tenantPackageMapper.selectOne(
                new LambdaQueryWrapper<TenantPackageEntity>()
                        .select(TenantPackageEntity::getId)
                        // 并非原地更新
                        .ne(Objects.nonNull(request.getId()), TenantPackageEntity::getId, request.getId())
                        // 套餐编码相同
                        .eq(TenantPackageEntity::getCode, request.getCode())
                        .last(SQLSegment.LIMIT_1)
        );

        if (entity != null) {
            throw new BusinessException(DefaultErrorCodeEnum.A00001, "已存在相同的套餐编码");
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
