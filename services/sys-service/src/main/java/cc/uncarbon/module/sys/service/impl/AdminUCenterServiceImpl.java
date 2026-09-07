package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.model.request.AdminUpdateMyPasswordRequest;
import cc.uncarbon.module.sys.model.request.AdminUpdateMyProfileRequest;
import cc.uncarbon.module.sys.model.valueobj.MyProfileDTO;
import cc.uncarbon.module.sys.service.AdminUCenterService;
import cc.uncarbon.module.sys.service.SysUserService;
import cc.uncarbon.module.sys.util.PwdUtil;
import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 后台管理-用户中心
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class AdminUCenterServiceImpl implements AdminUCenterService {

    private final SysUserService sysUserService;
    private final SysUserMapper sysUserMapper;


    @Override
    public MyProfileDTO getMyProfile() {
        var me = sysUserService.getNonnullEntityById(UserContextHolder.getUserId());
        var ret = new MyProfileDTO();
        BeanUtil.copyProperties(me, ret);
        return ret;
    }

    @Override
    public void updateMyPassword(AdminUpdateMyPasswordRequest request) {
        if (!Objects.equals(request.getNeo(), request.getConfirmNeo())) {
            throw new BusinessException(SysErrorCodeEnum.A01003);
        }
        Long userId = UserContextHolder.getUserId();
        assert userId != null;
        var entity = sysUserMapper.selectById(userId);
        if (entity == null || !entity.getPwd().equals(PwdUtil.encrypt(request.getOld(), entity.getPwdSalt()))) {
            throw new BusinessException(SysErrorCodeEnum.A01004);
        }
        final String encryptPwd = PwdUtil.encrypt(request.getNeo(), entity.getPwdSalt());
        if (entity.getPwd().equals(encryptPwd)) {
            throw new BusinessException(SysErrorCodeEnum.A01007);
        }
        sysUserMapper.updateEncryptedPwd(userId, encryptPwd);
    }

    @Override
    public void updateMyProfile(AdminUpdateMyProfileRequest request) {
        var entity = new SysUserEntity();
        BeanUtil.copyProperties(request, entity);
        entity.setId(UserContextHolder.getUserId());
        sysUserMapper.updateById(entity);
    }

    @Override
    public void updateMyAvatar(String url) {
        var entity = new SysUserEntity().setId(UserContextHolder.getUserId())
                .setAvatarUrl(url);
        sysUserMapper.updateById(entity);
    }
}
