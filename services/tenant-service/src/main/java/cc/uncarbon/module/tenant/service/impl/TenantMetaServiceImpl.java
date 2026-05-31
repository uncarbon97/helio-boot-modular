package cc.uncarbon.module.tenant.service.impl;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.db.constant.SQLSegment;
import cc.uncarbon.module.tenant.dal.entity.TenantMetaEntity;
import cc.uncarbon.module.tenant.dal.mapper.TenantMetaMapper;
import cc.uncarbon.module.tenant.enums.TenantErrorCodeEnum;
import cc.uncarbon.module.tenant.model.query.AdminTenantMetaListQuery;
import cc.uncarbon.module.tenant.model.request.AdminCreateTenantRequest;
import cc.uncarbon.module.tenant.model.request.AdminUpdateTenantMetaRequest;
import cc.uncarbon.module.tenant.model.valueobj.TenantMetaDTO;
import cc.uncarbon.module.tenant.service.TenantMetaService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * 租户主数据
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class TenantMetaServiceImpl implements TenantMetaService {

    private static final String LOG_PREFIX = "[租户管理][租户管理]";

    private final TenantMetaMapper tenantMetaMapper;


    @Override
    public PageResult<TenantMetaDTO> adminList(AdminTenantMetaListQuery query) {
        Page<TenantMetaEntity> entityPage = tenantMetaMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new QueryWrapper<TenantMetaEntity>()
                        .lambda()
                        // 租户编码
                        .like(ObjectUtil.isNotNull(query.getCode()), TenantMetaEntity::getCode, CharSequenceUtil.cleanBlank(query.getCode()))
                        // 租户名称
                        .like(CharSequenceUtil.isNotBlank(query.getName()), TenantMetaEntity::getName, CharSequenceUtil.cleanBlank(query.getName()))
                        // 状态
                        .eq(ObjectUtil.isNotNull(query.getStatus()), TenantMetaEntity::getStatus, query.getStatus())
                        // 排序
                        .orderByDesc(TenantMetaEntity::getId)
        );

        return convertPage(entityPage, true);
    }

    @Override
    public TenantMetaDTO getOneById(Long id) {
        return getOneById(id, false);
    }

    @Override
    public TenantMetaDTO getOneById(Long id, boolean throwIfInvalidId) throws BusinessException {
        TenantMetaEntity entity = tenantMetaMapper.selectById(id);
        if (throwIfInvalidId) {
            TenantErrorCodeEnum.A03001.throwIfNull(entity);
        }

        return convertEntity(entity, true);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public TenantMetaEntity adminCreate(AdminCreateTenantRequest request) {
        log.info(LOG_PREFIX + "新增 >> {}", request);

        TenantMetaEntity entity = new TenantMetaEntity();
        BeanUtil.copyProperties(request, entity);

        tenantMetaMapper.insert(entity);

        return entity;
    }

    /**
     * 系统管理-修改
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminUpdate(AdminUpdateTenantMetaRequest request) {
        log.info(LOG_PREFIX + "修改 >> {}", request);

        TenantMetaEntity entity = new TenantMetaEntity();
        BeanUtil.copyProperties(request, entity);

        tenantMetaMapper.updateById(entity);
    }

    /**
     * 系统管理-删除
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminDelete(Collection<Long> ids) {
        log.info("[系统管理-删除系统租户] >> 入参={}", ids);
        tenantMetaMapper.deleteByIds(ids);
    }

    /**
     * 根据主键IDs，取租户BOs
     * @param fillTenantAdminUser 是否根据租户管理员用户ID，查询关联用户信息并填充到BO
     */
    @Override
    public List<TenantMetaDTO> listByIds(Collection<Long> ids, boolean fillTenantAdminUser) {
        if (CollUtil.isEmpty(ids)) {
            return List.of();
        }
        List<TenantMetaEntity> entityList = tenantMetaMapper.selectBatchIds(ids);
        return convertList(entityList, fillTenantAdminUser);
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    /**
     * 实体转值对象
     * @param fillTenantAdminUser 是否根据租户管理员用户ID，查询关联用户信息并填充到BO
     */
    private TenantMetaDTO convertEntity(TenantMetaEntity entity, boolean fillTenantAdminUser) {
        if (entity == null) {
            return null;
        }

        TenantMetaDTO bo = new TenantMetaDTO();
        BeanUtil.copyProperties(entity, bo);

        // 按需改写字段
        if (fillTenantAdminUser && ObjectUtil.isNotNull(entity.getTenantAdminUserId())) {
            bo.setTenantAdminUser(sysUserMapper.getBaseInfoByUserId(entity.getTenantAdminUserId()));
        }

        return bo;
    }

    /**
     * 实体转值对象
     *
     * @param entityList 实体 List
     * @param fillTenantAdminUser 填充管理员用户信息
     */
    private List<TenantMetaDTO> convertList(List<TenantMetaEntity> entityList, boolean fillTenantAdminUser) {
        // 深拷贝
        List<TenantMetaDTO> ret = new ArrayList<>(entityList.size());
        entityList.forEach(
                entity -> ret.add(convertEntity(entity, fillTenantAdminUser))
        );

        return ret;
    }

    /**
     * 实体转值对象
     */
    private PageResult<TenantMetaDTO> convertPage(Page<TenantMetaEntity> entityPage, boolean fillTenantAdminUser) {
        return new PageResult<TenantMetaDTO>()
                .setCurrent(entityPage.getCurrent())
                .setSize(entityPage.getSize())
                .setTotal(entityPage.getTotal())
                .setRecords(convertList(entityPage.getRecords(), fillTenantAdminUser));
    }

    /**
     * 检查是否存在重复
     */
    private void checkRepeat(AdminCreateTenantRequest request) {
        TenantMetaEntity existingEntity = tenantMetaMapper.selectOne(
                new QueryWrapper<TenantMetaEntity>()
                        .lambda()
                        // 仅取主键ID
                        .select(TenantMetaEntity::getId)
                        // 租户ID相同
                        .eq(TenantMetaEntity::getTenantId, request.getTenantId())
                        .or()
                        // 或租户名相同
                        .eq(TenantMetaEntity::getTenantName, request.getTenantName())
                        .last(SQLSegment.LIMIT_1)
        );

        if (existingEntity != null && !existingEntity.getId().equals(request.getId())) {
            throw new BusinessException(400, "已存在相同系统租户，请重新输入");
        }
    }

}
