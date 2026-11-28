package cc.uncarbon.module.sys.biz;

import cc.uncarbon.framework.helium.i18n.util.I18nMessageUtil;
import cc.uncarbon.module.sys.MybatisPlusTestSupport;
import cc.uncarbon.module.sys.dal.entity.SysDeptEntity;
import cc.uncarbon.module.sys.dal.entity.SysOperateLogEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.model.valueobj.SysDeptDTO;
import cc.uncarbon.module.sys.model.valueobj.SysOperateLogDTO;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

import java.util.List;
import java.util.Locale;

/**
 * {@link AuditNicknameFillerImpl} 批量填充、回退与国际化文案测试
 */
@ExtendWith(MockitoExtension.class)
class AuditNicknameFillerImplTest {

    private SysUserMapper sysUserMapper;
    private AuditNicknameFillerImpl filler;

    @BeforeAll
    static void initTableInfoCache() {
        MybatisPlusTestSupport.initTableInfo();
    }

    @BeforeEach
    void setUp() {
        sysUserMapper = Mockito.mock(SysUserMapper.class);
        filler = new AuditNicknameFillerImpl(sysUserMapper);

        // 模拟未命中任何语言包，走硬编码兜底文案（部分用例不会触发，宽松打桩避免 STRICT_STUBS 误报）
        MessageSource messageSource = Mockito.mock(MessageSource.class);
        Mockito.lenient().when(messageSource.getMessage(Mockito.anyString(), Mockito.any(), Mockito.any(Locale.class)))
                .thenThrow(new NoSuchMessageException("no such message"));
        I18nMessageUtil.setMessageSource(messageSource);
    }


    /*
    ----------------------------------------------------------------
                        createdBy / updatedBy 填充
    ----------------------------------------------------------------
     */

    @Test
    void fillHitsNicknameAndMissesToDeletedText() {
        Mockito.when(sysUserMapper.selectList(Mockito.<Wrapper<SysUserEntity>>any()))
                .thenReturn(List.of(user(1L, "小明", null)));

        SysDeptEntity first = new SysDeptEntity();
        first.setCreatedBy(1L).setUpdatedBy(2L);
        SysDeptEntity second = new SysDeptEntity();
        second.setCreatedBy(1L);

        List<SysDeptDTO> ret = filler.fill(List.of(first, second), entity -> new SysDeptDTO());

        Assertions.assertEquals(2, ret.size());
        Assertions.assertEquals("小明", ret.get(0).getCreatedBy());
        Assertions.assertEquals("已删除用户(2)", ret.get(0).getUpdatedBy());
        Assertions.assertEquals("小明", ret.get(1).getCreatedBy());
        Assertions.assertNull(ret.get(1).getUpdatedBy());
        // 一批数据源只允许一次 sys_user in 查询
        Mockito.verify(sysUserMapper).selectList(Mockito.<Wrapper<SysUserEntity>>any());
    }

    @Test
    void nullUserIdKeepsNullNicknameWithoutQuery() {
        List<SysDeptDTO> ret = filler.fill(List.of(new SysDeptEntity()), entity -> new SysDeptDTO());

        Assertions.assertEquals(1, ret.size());
        Assertions.assertNull(ret.getFirst().getCreatedBy());
        Assertions.assertNull(ret.getFirst().getUpdatedBy());
        Mockito.verifyNoInteractions(sysUserMapper);
    }

    @Test
    void blankNicknameFallsBackToPin() {
        Mockito.when(sysUserMapper.selectList(Mockito.<Wrapper<SysUserEntity>>any()))
                .thenReturn(List.of(user(9L, "", "zhangsan")));

        SysDeptEntity entity = new SysDeptEntity();
        entity.setCreatedBy(9L);
        List<SysDeptDTO> ret = filler.fill(List.of(entity), _ -> new SysDeptDTO());

        Assertions.assertEquals("zhangsan", ret.getFirst().getCreatedBy());
    }

    @Test
    void emptyCollectionIsNoOp() {
        Assertions.assertTrue(filler.fill(List.of(), _ -> new SysDeptDTO()).isEmpty());
        Mockito.verifyNoInteractions(sysUserMapper);
    }

    @Test
    void fillOneNullSourceYieldsNullWithoutQuery() {
        Assertions.assertNull(filler.fillOne(null, _ -> new SysDeptDTO()));
        Mockito.verifyNoInteractions(sysUserMapper);
    }

    @Test
    void fillOneResolvesBothNicknames() {
        Mockito.when(sysUserMapper.selectList(Mockito.<Wrapper<SysUserEntity>>any()))
                .thenReturn(List.of(user(1L, "小明", null)));

        SysDeptEntity entity = new SysDeptEntity();
        entity.setCreatedBy(1L).setUpdatedBy(1L);
        SysDeptDTO dto = filler.fillOne(entity, _ -> new SysDeptDTO());

        Assertions.assertEquals("小明", dto.getCreatedBy());
        Assertions.assertEquals("小明", dto.getUpdatedBy());
    }

    @Test
    void fillByUserIdOneNullSourceYieldsNullWithoutQuery() {
        Assertions.assertNull(filler.fillByUserIdOne(null, entity -> new SysOperateLogDTO()));
        Mockito.verifyNoInteractions(sysUserMapper);
    }


    /*
    ----------------------------------------------------------------
                        userId 填充（操作日志）
    ----------------------------------------------------------------
     */

    @Test
    void fillByUserIdResolvesNickname() {
        Mockito.when(sysUserMapper.selectList(Mockito.any()))
                .thenReturn(List.of(user(1001L, "运营小编", null)));

        List<SysOperateLogDTO> ret = filler.fillByUserId(
                List.of(new SysOperateLogEntity().setUserId(1001L)), entity -> new SysOperateLogDTO());

        Assertions.assertEquals("运营小编", ret.getFirst().getUserNickname());
    }

    @Test
    void fillByUserIdMissesToDeletedText() {
        Mockito.when(sysUserMapper.selectList(Mockito.any()))
                .thenReturn(List.of());

        List<SysOperateLogDTO> ret = filler.fillByUserId(
                List.of(new SysOperateLogEntity().setUserId(404L)), entity -> new SysOperateLogDTO());

        Assertions.assertEquals("已删除用户(404)", ret.getFirst().getUserNickname());
    }


    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    private SysUserEntity user(Long id, String nickname, String pin) {
        return new SysUserEntity()
                .setId(id)
                .setNickname(nickname)
                .setPin(pin);
    }
}
