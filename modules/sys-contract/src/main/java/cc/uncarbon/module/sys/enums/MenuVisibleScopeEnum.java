package cc.uncarbon.module.sys.enums;

import cc.uncarbon.framework.helium.base.enums.BaseEnum;
import cc.uncarbon.module.commons.enumdict.EnumDict;
import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 预置菜单可见范围枚举
 * <p>
 * 标记为仅超管可见的菜单行，其自身及全部子孙菜单：
 * 对超级管理员以外的人群不可见（列表、详情、路由、权限串均剔除），
 * 且不允许绑定给系统角色或租户套餐；需先调整为通用可见，再进行授权
 */
@EnumDict(name = "菜单可见范围")
@AllArgsConstructor
@Getter
public enum MenuVisibleScopeEnum implements BaseEnum<Integer> {

    /**
     * 通用，按常规角色授权关系控制可见性
     */
    ALL(1, "通用"),

    /**
     * 仅超级管理员可见
     */
    SUPER_ADMIN_ONLY(2, "仅超级管理员"),;

    @EnumValue
    private final Integer value;
    private final String label;

}
