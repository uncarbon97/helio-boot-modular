package cc.uncarbon.module.sys.biz;

import cc.uncarbon.module.sys.facade.SysMenuFacade;
import cc.uncarbon.module.sys.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 系统菜单门面
 */
@RequiredArgsConstructor
@Service
public class SysMenuFacadeImpl implements SysMenuFacade {

    private final SysMenuService sysMenuService;

    @Override
    public Set<Long> listSuperAdminOnlySubtreeMenuIds() {
        return sysMenuService.listSuperAdminOnlySubtreeMenuIds();
    }

}
