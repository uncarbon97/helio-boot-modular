package cc.uncarbon.module.tenant.service.impl;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.db.constant.SQLSegment;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.commons.exception.HasRepeatRecordException;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.sys.facade.TenantUserRoleFacade;
import cc.uncarbon.module.sys.model.request.TenantUserBindRoleRequest;
import cc.uncarbon.module.sys.model.request.TenantRoleCreateRequest;
import cc.uncarbon.module.sys.model.request.TenantUserCreateRequest;
import cc.uncarbon.module.tenant.dal.entity.TenantMetaEntity;
import cc.uncarbon.module.tenant.dal.mapper.TenantMetaMapper;
import cc.uncarbon.module.tenant.enums.TenantErrorCodeEnum;
import cc.uncarbon.module.tenant.model.query.AdminTenantMetaListQuery;
import cc.uncarbon.module.tenant.model.request.AdminTenantCreateRequest;
import cc.uncarbon.module.tenant.model.request.AdminTenantMetaUpdateRequest;
import cc.uncarbon.module.tenant.model.valueobj.TenantMetaDTO;
import cc.uncarbon.module.tenant.model.valueobj.TenantPackageDTO;
import cc.uncarbon.module.tenant.service.TenantPackageService;
import cc.uncarbon.module.tenant.service.TenantService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;


/**
 * 租户元数据
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class TenantServiceImpl implements TenantService {

    private static final String LOG_PREFIX = "[租户管理][租户管理]";

    private final TenantMetaMapper tenantMetaMapper;
    private final TenantPackageService tenantPackageService;
    private final TenantUserRoleFacade tenantUserRoleFacade;


    @Override
    public PageResult<TenantMetaDTO> adminList(AdminTenantMetaListQuery query) {
        Page<TenantMetaEntity> entityPage = tenantMetaMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<TenantMetaEntity>()
                        // 租户编码
                        .like(Objects.nonNull(query.getCode()), TenantMetaEntity::getCode, CharSequenceUtil.cleanBlank(query.getCode()))
                        // 租户名称
                        .like(CharSequenceUtil.isNotBlank(query.getName()), TenantMetaEntity::getName, CharSequenceUtil.cleanBlank(query.getName()))
                        // 状态
                        .eq(Objects.nonNull(query.getStatus()), TenantMetaEntity::getStatus, query.getStatus())
                        // 排序
                        .orderByDesc(TenantMetaEntity::getId)
        );

        return convertPage(entityPage, true);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long adminCreate(AdminTenantCreateRequest request) {
        log.info(LOG_PREFIX + "新增 >> {}", request);
        checkRepeat(request);
        TenantPackageDTO pkg = checkPackage(request.getPackageId());

        TenantMetaEntity entity = new TenantMetaEntity();
        BeanUtil.copyProperties(request, entity);

        tenantMetaMapper.insert(entity);

        initTenant(request, entity, pkg);
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminUpdate(AdminTenantMetaUpdateRequest request) {
        log.info(LOG_PREFIX + "修改 >> {}", request);
        checkExistence(request.getId());

        TenantMetaEntity entity = new TenantMetaEntity();
        BeanUtil.copyProperties(request, entity);

        tenantMetaMapper.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminDelete(Collection<Long> ids) {
        log.info("[系统管理-删除系统租户] >> 入参={}", ids);
        tenantMetaMapper.deleteByIds(ids);
    }

    @Override
    public TenantMetaDTO getById(Long id) {
        if (id == null) return null;
        TenantMetaEntity entity = tenantMetaMapper.selectById(id);
        return convertEntity(entity, true);
    }

    @Override
    public TenantMetaDTO getNonnullById(Long id) throws NoRecordException {
        return NoRecordException.throwIfNull(getById(id));
    }

    /**
     * 根据主键IDs，取租户BOs
     *
     * @param fillTenantAdminUser 是否根据租户管理员用户ID，查询关联用户信息并填充到BO
     */
    @Override
    public List<TenantMetaDTO> listByIds(Collection<Long> ids, boolean fillTenantAdminUser) {
        if (CollUtil.isEmpty(ids)) {
            return List.of();
        }
        List<TenantMetaEntity> entityList = tenantMetaMapper.selectByIds(ids);
        return convertList(entityList, fillTenantAdminUser);
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    /**
     * 实体转值对象
     *
     * @param fillTenantAdminUser 是否根据租户管理员用户ID，查询关联用户信息并填充到BO
     */
    private TenantMetaDTO convertEntity(TenantMetaEntity entity, boolean fillTenantAdminUser) {
        if (entity == null) {
            return null;
        }

        TenantMetaDTO ret = new TenantMetaDTO();
        BeanUtil.copyProperties(entity, ret);
        // 按需改写字段
        if (fillTenantAdminUser && entity.getAdminUserId() != null) {
            ret.setAdminUserProfile(tenantUserRoleFacade.getTenantUserBasicProfile(
                    entity.getId(), entity.getAdminUserId()));
        }

        return ret;
    }

    /**
     * 实体转值对象
     *
     * @param entityList          实体 List
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
    private void checkRepeat(AdminTenantCreateRequest request) {
        TenantMetaEntity entity = tenantMetaMapper.selectOne(new LambdaQueryWrapper<TenantMetaEntity>()
                .select(TenantMetaEntity::getId)
                // 租户编码相同
                .eq(TenantMetaEntity::getCode, request.getCode())
                .last(SQLSegment.LIMIT_1)
        );

        if (entity != null) {
            throw new HasRepeatRecordException("已存在相同的租户编码");
        }
    }

    /**
     * 检查是否存在
     */
    private void checkExistence(Long id) {
        boolean exists = tenantMetaMapper.exists(
                new LambdaQueryWrapper<TenantMetaEntity>()
                        .select(TenantMetaEntity::getId)
                        .eq(TenantMetaEntity::getId, id)
                        .last(SQLSegment.LIMIT_1)
        );
        NoRecordException.throwIfFalse(exists);
    }

    /**
     * 检查租户套餐
     */
    private TenantPackageDTO checkPackage(Long packageId) {
        if (packageId != null) {
            TenantPackageDTO pkg = tenantPackageService.getNonnullById(packageId);
            if (pkg.getStatus() != EnabledStatusEnum.ENABLED) {
                throw new BusinessException(TenantErrorCodeEnum.A03001);
            }
            return pkg;
        }
        return null;
    }

    /**
     * 初始化新租户
     */
    private void initTenant(@NonNull AdminTenantCreateRequest request,
                            @NonNull TenantMetaEntity entity,
                            @NonNull TenantPackageDTO pkg) {
        long tenantId = entity.getId();
        String tenantCode = entity.getCode();

        // 创建租户管理员角色
        var tenantRole = tenantUserRoleFacade.createTenantRole(new TenantRoleCreateRequest()
                .setTenantId(tenantId)
                .setTenantCode(tenantCode)
                .setStatus(EnabledStatusEnum.ENABLED)
                .setTenantAdmin(true)
        );

        // TODO 根据租户套餐，绑定租户管理员角色-菜单关联关系

        // 创建租户管理员用户
        var tenantUser = tenantUserRoleFacade.createTenantUser(new TenantUserCreateRequest()
                .setTenantId(tenantId)
                .setTenantCode(tenantCode)
                .setTenantName(entity.getName())
                .setPin(request.getTenantAdminPin())
                .setPwdPlain(request.getTenantAdminPwd())
                .setEmail(request.getTenantAdminEmail())
                .setPhoneNo(request.getTenantAdminPhoneNo())
                .setTenantAdmin(true)
        );

        // 绑定租户管理员用户-角色关联关系
        tenantUserRoleFacade.bindTenantUserRoleRelation(new TenantUserBindRoleRequest()
                .setTenantId(tenantId)
                .setTenantCode(tenantCode)
                .setUserId(tenantUser.getNewUserId())
                .setRoleIds(List.of(tenantRole.getNewRoleId()))
        );

        // 更新租户主数据，把租户管理员用户ID 记下来
        tenantMetaMapper.updateAdminUserId(tenantId, tenantUser.getNewUserId());
    }

}
