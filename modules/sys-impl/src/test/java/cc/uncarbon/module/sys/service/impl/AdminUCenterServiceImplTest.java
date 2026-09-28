package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.context.SimpleUserContext;
import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.db.enums.GenderEnum;
import cc.uncarbon.framework.helium.db.enums.YesOrNoEnum;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.model.request.AdminUpdateMyPasswordRequest;
import cc.uncarbon.module.sys.model.request.AdminUpdateMyProfileRequest;
import cc.uncarbon.module.sys.model.valueobj.MyProfileDTO;
import cc.uncarbon.module.sys.model.valueobj.SysUserBasicProfileDTO;
import cc.uncarbon.module.sys.service.SysUserService;
import cc.uncarbon.module.sys.util.PwdUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;

/**
 * {@link AdminUCenterServiceImpl} 个人中心分支测试
 */
@ExtendWith(MockitoExtension.class)
class AdminUCenterServiceImplTest {

    private static final String OLD_PWD = "old-pass-123";
    private static final String NEW_PWD = "new-pass-456";

    @Mock
    private SysUserService sysUserService;
    @Mock
    private SysUserMapper sysUserMapper;

    @InjectMocks
    private AdminUCenterServiceImpl service;


    @Test
    void getMyProfileCopiesBasicProfile() {
        Mockito.when(sysUserService.getNonnullBasicProfileById(3L)).thenReturn(
                new SysUserBasicProfileDTO().setId(3L).setPin("u3").setNickname("n3")
                        .setEmail("a@b.c").setPhoneNo("13800000000"));

        MyProfileDTO ret = withUserReturn(3L, () -> service.getMyProfile());

        Assertions.assertEquals("u3", ret.getPin());
        Assertions.assertEquals("n3", ret.getNickname());
        Assertions.assertEquals("a@b.c", ret.getEmail());
    }

    @Test
    void updateMyPasswordMismatchedConfirmRejected() {
        var request = new AdminUpdateMyPasswordRequest()
                .setOld(OLD_PWD).setNeo(NEW_PWD).setConfirmNeo("different-9");

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> withUser(3L, () -> service.updateMyPassword(request)));
        Assertions.assertEquals(SysErrorCodeEnum.A01003, ex.getErrorCode());
    }

    @Test
    void updateMyPasswordMissingUserRejected() {
        Mockito.when(sysUserMapper.selectById(3L)).thenReturn(null);

        var request = new AdminUpdateMyPasswordRequest()
                .setOld(OLD_PWD).setNeo(NEW_PWD).setConfirmNeo(NEW_PWD);
        var ex = Assertions.assertThrows(BusinessException.class,
                () -> withUser(3L, () -> service.updateMyPassword(request)));
        Assertions.assertEquals(SysErrorCodeEnum.A01004, ex.getErrorCode());
    }

    @Test
    void updateMyPasswordWrongOldPwdRejected() {
        Mockito.when(sysUserMapper.selectById(3L)).thenReturn(
                new SysUserEntity().setId(3L).setPwd(PwdUtil.hash(OLD_PWD)));

        var request = new AdminUpdateMyPasswordRequest()
                .setOld("wrong-pass-0").setNeo(NEW_PWD).setConfirmNeo(NEW_PWD);
        var ex = Assertions.assertThrows(BusinessException.class,
                () -> withUser(3L, () -> service.updateMyPassword(request)));
        Assertions.assertEquals(SysErrorCodeEnum.A01004, ex.getErrorCode());
    }

    @Test
    void updateMyPasswordHappyPath() {
        Mockito.when(sysUserMapper.selectById(3L)).thenReturn(
                new SysUserEntity().setId(3L).setPwd(PwdUtil.hash(OLD_PWD)));

        var request = new AdminUpdateMyPasswordRequest()
                .setOld(OLD_PWD).setNeo(NEW_PWD).setConfirmNeo(NEW_PWD);
        withUser(3L, () -> service.updateMyPassword(request));

        ArgumentCaptor<String> pwdCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(sysUserMapper).updateEncryptedPwd(
                Mockito.eq(3L), pwdCaptor.capture(), Mockito.eq(YesOrNoEnum.NO));
        Assertions.assertTrue(pwdCaptor.getValue().startsWith("$argon2id$"));
        // 注：A01007（新旧密码相同）依赖哈希碰撞，随机盐下不可达，不测试
    }

    @Test
    void updateMyProfileWritesCurrentUserId() {
        var request = new AdminUpdateMyProfileRequest()
                .setNickname("新昵称").setGender(GenderEnum.MALE).setEmail("a@b.c").setPhoneNo("13800000000");

        withUser(3L, () -> service.updateMyProfile(request));

        ArgumentCaptor<SysUserEntity> captor = ArgumentCaptor.forClass(SysUserEntity.class);
        Mockito.verify(sysUserMapper).updateById(captor.capture());
        Assertions.assertEquals(3L, captor.getValue().getId());
        Assertions.assertEquals("新昵称", captor.getValue().getNickname());
    }

    @Test
    void updateMyAvatarWritesCurrentUserId() {
        withUser(3L, () -> service.updateMyAvatar("https://cdn/x.png"));

        ArgumentCaptor<SysUserEntity> captor = ArgumentCaptor.forClass(SysUserEntity.class);
        Mockito.verify(sysUserMapper).updateById(captor.capture());
        Assertions.assertEquals(3L, captor.getValue().getId());
        Assertions.assertEquals("https://cdn/x.png", captor.getValue().getAvatarUrl());
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private static void withUser(Long userId, Runnable op) {
        ScopedValue.where(UserContextHolder.scoped(), new SimpleUserContext().setUserId(userId)).run(op);
    }

    private static <T> T withUserReturn(Long userId, Supplier<T> op) {
        var ref = new Object() {
            T value;
        };
        withUser(userId, () -> ref.value = op.get());
        return ref.value;
    }
}
