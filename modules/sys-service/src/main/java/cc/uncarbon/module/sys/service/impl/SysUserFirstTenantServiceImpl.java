package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.db.fieldcrypto.blindindex.BlindIndexCalculator;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.framework.helium.tenant.enums.TenantLoginModeEnum;
import cc.uncarbon.module.commons.constant.SQLSegment;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.sys.constant.SysUserCipherCols;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserTenantRelationEntity;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserTenantRelationMapper;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.facade.TenantUserRoleFacade;
import cc.uncarbon.module.sys.model.request.TenantUserBindRoleRequest;
import cc.uncarbon.module.sys.model.request.TenantUserExpelRequest;
import cc.uncarbon.module.sys.model.request.TenantUserJoinRequest;
import cc.uncarbon.module.sys.model.request.TenantUserLookupRequest;
import cc.uncarbon.module.sys.model.valueobj.SysUserDTO;
import cc.uncarbon.module.sys.model.valueobj.TenantUserLookupVO;
import cc.uncarbon.module.sys.service.SysUserDeptRelationService;
import cc.uncarbon.module.sys.service.SysUserFirstTenantService;
import cc.uncarbon.module.sys.service.SysUserRoleRelationService;
import cc.uncarbon.module.sys.util.PiiContractNormalizer;
import cc.uncarbon.module.sys.util.SysUtil;
import cc.uncarbon.module.tenant.constant.TenantConstant;
import cc.uncarbon.module.tenant.errorcode.TenantErrorCodeEnum;
import cc.uncarbon.module.tenant.facade.TenantConfigFacade;
import cc.uncarbon.module.tenant.facade.TenantFacade;
import cc.uncarbon.module.tenant.util.TenantUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * USER_FIRST 模式下，租户成员资格服务
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysUserFirstTenantServiceImpl implements SysUserFirstTenantService {

    private static final String LOG_PREFIX = "[系统管理][租户成员]";

    private final SysUserMapper sysUserMapper;
    private final SysUserTenantRelationMapper sysUserTenantRelationMapper;
    private final SysUserRoleRelationService sysUserRoleRelationService;
    private final SysUserDeptRelationService sysUserDeptRelationService;
    private final TenantUserRoleFacade tenantUserRoleFacade;
    private final TenantFacade tenantFacade;
    private final TenantConfigFacade tenantConfigFacade;
    private final BlindIndexCalculator blindIndexCalculator;


    @Override
    public List<TenantUserLookupVO> lookupUsers(TenantUserLookupRequest request) {
        assertUserFirstMode();
        long tenantId = currentTenantId();
        // 同一手机号可对应多账号，仅保留可用候选人
        List<SysUserEntity> usableUsers = findGlobalUsers(request).stream()
                .filter(user -> !isUnusableLookupTarget(user))
                .toList();
        if (usableUsers.isEmpty()) {
            throw new BusinessException(SysErrorCodeEnum.A01046);
        }
        List<TenantUserLookupVO> ret = usableUsers.stream().map(user -> new TenantUserLookupVO()
                        .setUserId(user.getId())
                        .setPin(user.getPin())
                        .setNickname(user.getNickname())
                        .setPhoneNo(user.getPhoneNoMasked())
                        .setEmail(user.getEmailMasked())
                        .setMemberFlag(existsRelation(tenantId, user.getId())))
                .toList();
        log.info(LOG_PREFIX + "全局找人 >> tenantId={}, userIds={}", tenantId,
                ret.stream().map(TenantUserLookupVO::getUserId).toList());
        return ret;
    }

    @Transactional(rollbackFor = Exception.class)
    @SneakyThrows
    @Override
    public void joinTenant(TenantUserJoinRequest request) {
        assertUserFirstMode();
        long tenantId = currentTenantId();
        if (tenantFacade.resolveEnabledTenant(tenantId) == null) {
            // 租户中途被禁用时不再进人
            throw new BusinessException(TenantErrorCodeEnum.A03006);
        }

        SysUserEntity user = TenantContextHolder.callIgnored(() -> sysUserMapper.getByPin(request.getPin()));
        if (user == null || SysUserStatusEnum.DISABLED == user.getStatus()) {
            throw new BusinessException(SysErrorCodeEnum.A01046);
        }
        if (SysUtil.isSuperAdmin(user.getId())) {
            throw new BusinessException(SysErrorCodeEnum.A01047);
        }
        if (existsRelation(tenantId, user.getId())) {
            throw new BusinessException(SysErrorCodeEnum.A01048);
        }

        try {
            sysUserTenantRelationMapper.insert(SysUserTenantRelationEntity.of(tenantId, user.getId()));
        } catch (DuplicateKeyException e) {
            throw new BusinessException(SysErrorCodeEnum.A01048);
        }

        if (CollUtil.isNotEmpty(request.getRoleIds())) {
            tenantUserRoleFacade.bindTenantUserRoleRelation(new TenantUserBindRoleRequest()
                    .setTenantId(tenantId)
                    .setTenantCode(TenantContextHolder.getTenantCode())
                    .setUserId(user.getId())
                    .setRoleIds(request.getRoleIds()));
        }
        log.info(LOG_PREFIX + "拉入本租户 >> tenantId={}, userId={}, pin={}", tenantId, user.getId(), user.getPin());
    }

    @Transactional(rollbackFor = Exception.class)
    @SneakyThrows
    @Override
    public SysUserDTO expelTenantMember(TenantUserExpelRequest request) {
        assertUserFirstMode();
        long tenantId = currentTenantId();
        long userId = request.getUserId();

        if (SysUtil.isSuperAdmin(userId)) {
            throw new BusinessException(SysErrorCodeEnum.A01047);
        }
        if (Objects.equals(userId, tenantFacade.getTenantAdminUserId(tenantId))) {
            // 防自断手脚：租户主管理员不可移除
            throw new BusinessException(SysErrorCodeEnum.A01049);
        }
        // 全局口径取被移出用户，供操作日志展示 pin；成员投影列可能指向他租户，不可走行级过滤
        SysUserEntity user = NoRecordException.throwIfNull(
                TenantContextHolder.callIgnored(() -> sysUserMapper.selectById(userId)));
        if (!existsRelation(tenantId, userId)) {
            // 幂等：非本租户成员
            return toLogDTO(user);
        }

        // 删真源行
        sysUserTenantRelationMapper.deleteByTenantIdAndUserId(tenantId, userId);

        // 清理本租户内角色/部门关系；行级过滤自动限定本租户行
        sysUserRoleRelationService.cleanAndBindByUser(userId, null);
        sysUserDeptRelationService.cleanAndBind(userId, null);

        // 投影纠偏：仅当记忆租户指向被移除租户时改指首个剩余启用租户；无剩余回落平台自营域
        if (Objects.equals(tenantUserRoleFacade.getUserHomeTenantId(userId), tenantId)) {
            List<Long> remaining = tenantUserRoleFacade.listEnabledTenantIdsByUser(userId);
            Long nextTenantId = remaining.isEmpty() ? TenantConstant.PLATFORM_TENANT_ID : remaining.getFirst();
            TenantContextHolder.runIgnored(() -> sysUserMapper.updateTenantId(userId, nextTenantId));
        }
        log.info(LOG_PREFIX + "移出租户 >> tenantId={}, userId={}, pin={}", tenantId, userId, user.getPin());
        return toLogDTO(user);
    }

    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    private void assertUserFirstMode() {
        if (TenantLoginModeEnum.USER_FIRST != tenantConfigFacade.getLoginMode()) {
            throw new BusinessException(SysErrorCodeEnum.A01045);
        }
    }

    /**
     * 取当前会话租户；严格模式下，请求必须带有租户上下文；平台自营域无租户成员概念
     */
    private static long currentTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (TenantUtil.isPlatformDomain(tenantId)) {
            throw new BusinessException(SysErrorCodeEnum.A01052);
        }
        return tenantId;
    }

    /**
     * 全局找人：账号精确匹配优先；完整手机号经全串盲索引精确匹配，同一手机号可对应多个账号
     */
    @SneakyThrows
    private List<SysUserEntity> findGlobalUsers(TenantUserLookupRequest request) {
        if (CharSequenceUtil.isNotBlank(request.getPin())) {
            SysUserEntity user = TenantContextHolder.callIgnored(() -> sysUserMapper.getByPin(request.getPin()));
            return user == null ? List.of() : List.of(user);
        }
        if (CharSequenceUtil.isNotBlank(request.getPhoneNo())) {
            String blindIndex = blindIndexCalculator.compute(SysUserCipherCols.PHONE_FULL_PURPOSE,
                    PiiContractNormalizer.normalizePhone(request.getPhoneNo()));
            return TenantContextHolder.callIgnored(() -> sysUserMapper.selectList(
                    new LambdaQueryWrapper<SysUserEntity>()
                            .eq(SysUserEntity::getPhoneNoFullBlindIndex, blindIndex)
                            .orderByAsc(SysUserEntity::getId)
                            .last(SQLSegment.LIMIT_5)));
        }
        throw new BusinessException(SysErrorCodeEnum.A01051);
    }

    /**
     * 找人目标是否不可用：不存在 / 已禁用 / 超级管理员
     */
    private boolean isUnusableLookupTarget(SysUserEntity user) {
        return user == null || SysUserStatusEnum.DISABLED == user.getStatus()
                || SysUtil.isSuperAdmin(user.getId());
    }

    private boolean existsRelation(long tenantId, long userId) {
        return sysUserTenantRelationMapper.existsByTenantIdAndUserId(tenantId, userId);
    }

    /**
     * 仅提取操作日志所需字段
     */
    private static SysUserDTO toLogDTO(SysUserEntity user) {
        return new SysUserDTO()
                .setId(user.getId())
                .setPin(user.getPin())
                .setNickname(user.getNickname());
    }
}
