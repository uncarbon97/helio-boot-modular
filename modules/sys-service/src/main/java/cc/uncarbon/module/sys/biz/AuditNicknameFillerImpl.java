package cc.uncarbon.module.sys.biz;

import cc.uncarbon.framework.helium.i18n.util.I18nMessageUtil;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.module.commons.facade.AuditNicknameFiller;
import cc.uncarbon.module.commons.model.valueobj.AuditUserDisplayAware;
import cc.uncarbon.module.commons.model.valueobj.AuditingUserSource;
import cc.uncarbon.module.commons.model.valueobj.OperateLogUserDisplayAware;
import cc.uncarbon.module.commons.model.valueobj.OperateLogUserSource;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;

/**
 * 审计操作人昵称批量填充器
 * <p>
 * 以数据源（entity）携带的操作人用户ID 为准，一次 in 查询批量解析昵称后回填值对象；
 * 逻辑删除用户（@TableLogic 查不到）落入「已删除用户({ID})」分支；
 * 昵称缺失时回退显示账号 pin；用户ID 为 null（系统写入）回填 null 由前端显示「-」；
 * 昵称查询按用户ID全局进行，忽略租户行级隔离（记录的操作人可能属于其他租户）
 *
 * @author Uncarbon
 */
@RequiredArgsConstructor
@Component
public class AuditNicknameFillerImpl implements AuditNicknameFiller {

    private static final String DELETED_USER_NICKNAME_MESSAGE_CODE = "sys.user.deleted-nickname";
    private static final String DELETED_USER_NICKNAME_DEFAULT = "已删除用户({})";

    private final SysUserMapper sysUserMapper;


    @Override
    public <E extends AuditingUserSource, T extends AuditUserDisplayAware> List<T> fill(
            Collection<E> sources, Function<E, T> converter) {
        List<E> sourceList = CollUtil.isEmpty(sources) ? List.of() : new ArrayList<>(sources);
        if (sourceList.isEmpty()) {
            return List.of();
        }

        // 按数据源顺序完成转换
        List<T> ret = new ArrayList<>(sourceList.size());
        Set<Long> userIds = new HashSet<>();
        for (E source : sourceList) {
            ret.add(source == null ? null : converter.apply(source));
            if (source != null) {
                collectUserId(userIds, source.getCreatedBy());
                collectUserId(userIds, source.getUpdatedBy());
            }
        }

        Map<Long, String> nicknameMap = mapIdToNickname(userIds);
        for (int i = 0; i < sourceList.size(); i++) {
            T dto = ret.get(i);
            if (dto == null) {
                continue;
            }
            dto.setCreatedBy(resolveNickname(nicknameMap, sourceList.get(i).getCreatedBy()));
            dto.setUpdatedBy(resolveNickname(nicknameMap, sourceList.get(i).getUpdatedBy()));
        }
        return ret;
    }

    @Override
    public <E extends OperateLogUserSource, T extends OperateLogUserDisplayAware> List<T> fillByUserId(
            Collection<E> sources, Function<E, T> converter) {
        List<E> sourceList = CollUtil.isEmpty(sources) ? List.of() : new ArrayList<>(sources);
        if (sourceList.isEmpty()) {
            return List.of();
        }

        List<T> ret = new ArrayList<>(sourceList.size());
        Set<Long> userIds = new HashSet<>();
        for (E source : sourceList) {
            ret.add(source == null ? null : converter.apply(source));
            if (source != null) {
                collectUserId(userIds, source.getUserId());
            }
        }

        Map<Long, String> nicknameMap = mapIdToNickname(userIds);
        for (int i = 0; i < sourceList.size(); i++) {
            T dto = ret.get(i);
            if (dto != null) {
                dto.setUserNickname(resolveNickname(nicknameMap, sourceList.get(i).getUserId()));
            }
        }
        return ret;
    }


    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */


    private void collectUserId(Set<Long> userIds, Long userId) {
        if (userId != null) {
            userIds.add(userId);
        }
    }

    /**
     * 用户ID -> 展示名（昵称优先，缺失回退账号 pin）；查不到的用户不入表
     */
    private Map<Long, String> mapIdToNickname(Set<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Map.of();
        }

        List<SysUserEntity> foundUsers = new ArrayList<>();
        // 按用户ID全局查询，忽略租户隔离；@TableLogic 自动排除已逻辑删除用户
        TenantContextHolder.runIgnored(() ->
                foundUsers.addAll(sysUserMapper.selectList(new LambdaQueryWrapper<SysUserEntity>()
                        .select(SysUserEntity::getId, SysUserEntity::getNickname, SysUserEntity::getPin)
                        .in(SysUserEntity::getId, userIds)))
        );

        Map<Long, String> ret = new HashMap<>(foundUsers.size());
        for (SysUserEntity user : foundUsers) {
            String display = CharSequenceUtil.isNotBlank(user.getNickname())
                    ? user.getNickname()
                    : user.getPin();
            if (CharSequenceUtil.isNotBlank(display)) {
                ret.put(user.getId(), display);
            }
        }
        return ret;
    }

    private String resolveNickname(Map<Long, String> nicknameMap, Long userId) {
        if (userId == null) {
            // 系统操作写入，保持 null，前端显示「-」
            return null;
        }
        String nickname = nicknameMap.get(userId);
        if (CharSequenceUtil.isNotBlank(nickname)) {
            return nickname;
        }
        // 已逻辑删除或物理不存在的用户
        return I18nMessageUtil.messageOf(DELETED_USER_NICKNAME_MESSAGE_CODE, DELETED_USER_NICKNAME_DEFAULT, userId);
    }
}
