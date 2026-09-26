package cc.uncarbon.module.tenant.service.impl;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.commons.constant.SQLSegment;
import cc.uncarbon.module.commons.exception.HasRepeatRecordException;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.commons.model.request.AdminSetStatusRequest;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.facade.TenantUserRoleFacade;
import cc.uncarbon.module.sys.model.request.TenantRoleBindMenuRequest;
import cc.uncarbon.module.sys.model.request.TenantRoleCreateRequest;
import cc.uncarbon.module.sys.model.request.TenantUserBindRoleRequest;
import cc.uncarbon.module.sys.model.request.TenantUserCreateRequest;
import cc.uncarbon.module.tenant.dal.entity.TenantMetaEntity;
import cc.uncarbon.module.tenant.dal.mapper.TenantMetaMapper;
import cc.uncarbon.module.tenant.errorcode.TenantErrorCodeEnum;
import cc.uncarbon.module.tenant.model.query.AdminTenantMetaListQuery;
import cc.uncarbon.module.tenant.model.request.AdminTenantCreateRequest;
import cc.uncarbon.module.tenant.model.request.AdminTenantMetaUpdateRequest;
import cc.uncarbon.module.tenant.model.valueobj.TenantMetaDTO;
import cc.uncarbon.module.tenant.model.valueobj.TenantPackageDTO;
import cc.uncarbon.module.tenant.model.valueobj.TenantUserBasicProfileVO;
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
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;


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
        var ret = convertPage(entityPage);
        fillTenantAdminUser(ret.getRecords());
        return ret;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long adminCreate(AdminTenantCreateRequest request) {
        log.info(LOG_PREFIX + "新增 >> {}", request);
        checkRepeat(request);
        TenantPackageDTO pkg = checkPackage(request.getPackageId());

        var entity = new TenantMetaEntity();
        BeanUtil.copyProperties(request, entity);

        tenantMetaMapper.insert(entity);

        initTenant(request, entity, pkg);
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Set<Long> adminUpdate(AdminTenantMetaUpdateRequest request) {
        log.info(LOG_PREFIX + "修改 >> {}", request);
        var old = tenantMetaMapper.selectById(request.getId());
        NoRecordException.throwIfNull(old);
        TenantPackageDTO pkg = checkPackage(request.getPackageId());

        var entity = new TenantMetaEntity();
        BeanUtil.copyProperties(request, entity);

        tenantMetaMapper.updateById(entity);

        if (Objects.equals(old.getPackageId(), request.getPackageId())) {
            return Set.of();
        }
        // 套餐发生变化，将该租户全部角色菜单裁剪至新套餐范围；套餐置空 = 清空
        return tenantUserRoleFacade.syncTenantRoleMenus(
                request.getId(),
                pkg != null ? pkg.getMenuIds() : Set.of()
        );
    }

    @Override
    public List<Long> adminSetStatus(AdminSetStatusRequest<Long, EnabledStatusEnum> request) {
        log.info(LOG_PREFIX + "修改状态 >> {}", request);
        NoRecordException.throwIfNull(tenantMetaMapper.selectById(request.getId()));
        if (EnabledStatusEnum.DISABLED == request.getNewStatus()
                && Objects.equals(request.getId(), SysConstant.PLATFORM_TENANT_ID)) {
            // 平台自营域不可禁用（B5 种子约定）
            throw new BusinessException(TenantErrorCodeEnum.A03013);
        }

        List<Long> tenantUserIds = List.of();
        if (EnabledStatusEnum.DISABLED == request.getNewStatus()) {
            // 禁用时，取该租户全部用户ID，供调用方强制登出
            tenantUserIds = tenantUserRoleFacade.listUserIdsByTenantId(request.getId(), null);
        }

        var entity = new TenantMetaEntity()
                .setId(request.getId())
                .setStatus(request.getNewStatus());
        tenantMetaMapper.updateById(entity);
        return tenantUserIds;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminDelete(Collection<Long> ids) {
        log.info("[系统管理-删除系统租户] >> 入参={}", ids);
        if (CollUtil.contains(ids, SysConstant.PLATFORM_TENANT_ID)) {
            // 平台自营域不可删除（B5 种子约定）
            throw new BusinessException(TenantErrorCodeEnum.A03013);
        }
        // 仍有用户的租户不可删除，避免残留可继续使用的会话与孤儿关联数据
        for (Long id : ids) {
            List<Long> tenantUserIds = tenantUserRoleFacade.listUserIdsByTenantId(id, null);
            if (CollUtil.isNotEmpty(tenantUserIds)) {
                throw new BusinessException(TenantErrorCodeEnum.A03007);
            }
        }
        tenantMetaMapper.deleteByIds(ids);
    }

    @Override
    public TenantMetaDTO getById(Long id, boolean fillTenantAdminUser) {
        if (id == null) return null;
        var entity = tenantMetaMapper.selectById(id);
        var ret = convertEntity(entity);
        if (ret != null && fillTenantAdminUser) {
            fillTenantAdminUser(List.of(ret));
        }
        return ret;
    }

    @Override
    public TenantMetaDTO getNonnullById(Long id, boolean fillTenantAdminUser) throws NoRecordException {
        return NoRecordException.throwIfNull(getById(id, fillTenantAdminUser));
    }

    @Override
    public List<TenantMetaDTO> listEnabled() {
        return convertList(tenantMetaMapper.selectList(new LambdaQueryWrapper<TenantMetaEntity>()
                .eq(TenantMetaEntity::getStatus, EnabledStatusEnum.ENABLED)
                .orderByAsc(TenantMetaEntity::getId)
        ));
    }

    @Override
    public TenantMetaDTO getByCode(String code, boolean fillTenantAdminUser) {
        if (CharSequenceUtil.isBlank(code)) return null;
        var entity = tenantMetaMapper.selectByCode(code);
        var ret = convertEntity(entity);
        if (ret != null && fillTenantAdminUser) {
            fillTenantAdminUser(List.of(ret));
        }
        return ret;
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    /**
     * 实体转值对象
     */
    private TenantMetaDTO convertEntity(TenantMetaEntity entity) {
        if (entity == null) return null;

        var ret = new TenantMetaDTO();
        BeanUtil.copyProperties(entity, ret);
        return ret;
    }

    /**
     * 实体转值对象
     */
    private List<TenantMetaDTO> convertList(List<TenantMetaEntity> entityList) {
        if (CollUtil.isEmpty(entityList)) {
            return List.of();
        }
        return entityList.stream().map(this::convertEntity).toList();
    }

    /**
     * 实体转值对象
     */
    private PageResult<TenantMetaDTO> convertPage(Page<TenantMetaEntity> entityPage) {
        return new PageResult<TenantMetaDTO>()
                .setCurrent(entityPage.getCurrent())
                .setSize(entityPage.getSize())
                .setTotal(entityPage.getTotal())
                .setRecords(convertList(entityPage.getRecords()));
    }

    /**
     * 检查是否存在重复
     */
    private void checkRepeat(AdminTenantCreateRequest request) {
        var entity = tenantMetaMapper.selectOne(new LambdaQueryWrapper<TenantMetaEntity>()
                .select(TenantMetaEntity::getId)
                // 租户编码相同
                .eq(TenantMetaEntity::getCode, request.getCode())
                .last(SQLSegment.LIMIT_1)
        );

        if (entity != null) {
            throw new HasRepeatRecordException(TenantErrorCodeEnum.A03005);
        }
    }

    /**
     * 填充管理员用户信息
     */
    private void fillTenantAdminUser(Collection<TenantMetaDTO> collection) {
        if (CollUtil.isNotEmpty(collection)) {
            for (TenantMetaDTO item : collection) {
                if (item != null && item.getAdminUserId() != null) {
                    var profile = tenantUserRoleFacade.getTenantUserBasicProfile(item.getId(), item.getAdminUserId());
                    var vo = new TenantUserBasicProfileVO();
                    BeanUtil.copyProperties(profile, vo);
                    item.setAdminUserProfile(vo);
                }
            }
        }
    }

    /**
     * 检查租户套餐
     */
    private TenantPackageDTO checkPackage(Long packageId) {
        if (packageId != null) {
            TenantPackageDTO pkg = tenantPackageService.getNonnullById(packageId);
            if (pkg.getStatus() != EnabledStatusEnum.ENABLED) {
                throw new BusinessException(TenantErrorCodeEnum.A03002);
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
                            @Nullable TenantPackageDTO pkg) {
        long tenantId = entity.getId();
        String tenantCode = entity.getCode();

        // 创建租户管理员角色
        var tenantRole = tenantUserRoleFacade.createTenantRole(new TenantRoleCreateRequest()
                .setTenantId(tenantId)
                .setTenantCode(tenantCode)
                .setStatus(EnabledStatusEnum.ENABLED)
                .setTenantAdmin(true)
        );

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

        if (pkg != null) {
            // 根据租户套餐，绑定租户管理员角色-菜单关联关系
            tenantUserRoleFacade.bindTenantRoleMenuRelation(new TenantRoleBindMenuRequest()
                    .setTenantId(tenantId)
                    .setTenantCode(tenantCode)
                    .setRoleId(tenantRole.getNewRoleId())
                    .setMenuIds(pkg.getMenuIds())
            );
        }
    }

}
