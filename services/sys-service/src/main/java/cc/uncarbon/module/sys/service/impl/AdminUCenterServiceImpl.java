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
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
        sysUserMapper.updateEncryptedPwd(userId, PwdUtil.encrypt(request.getConfirmNeo(), entity.getPwdSalt()));
    }

    @Override
    public void updateMyProfile(AdminUpdateMyProfileRequest request) {
        sysUserMapper.update(new LambdaUpdateWrapper<SysUserEntity>()
                .eq(SysUserEntity::getId, UserContextHolder.getUserId())
                .set(SysUserEntity::getNickname, request.getNickname())
                .set(SysUserEntity::getGender, request.getGender())
                .set(SysUserEntity::getEmail, request.getEmail())
                .set(SysUserEntity::getPhoneNo, request.getPhoneNo())
        );
    }

    @Override
    public void updateMyAvatar(String url) {
        sysUserMapper.update(new LambdaUpdateWrapper<SysUserEntity>()
                .eq(SysUserEntity::getId, UserContextHolder.getUserId())
                .set(SysUserEntity::getAvatarUrl, url)
        );
    }
}
