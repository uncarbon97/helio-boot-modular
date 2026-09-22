package cc.uncarbon.module.sys.facade;

import java.util.Set;

/**
 * 系统菜单门面
 * <p>
 * 供 sys 模块之外的模块（如租户管理）查询菜单保护信息
 */
public interface SysMenuFacade {

    /**
     * 列举「仅超管可见」菜单及全部子孙菜单IDs
     * <p>
     * 绑定菜单前校验使用，命中任意ID即应拒绝绑定
     */
    Set<Long> listSuperAdminOnlySubtreeMenuIds();

}
