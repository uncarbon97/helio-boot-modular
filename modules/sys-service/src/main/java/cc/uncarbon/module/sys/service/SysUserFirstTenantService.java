package cc.uncarbon.module.sys.service;

import cc.uncarbon.module.sys.model.request.TenantUserExpelRequest;
import cc.uncarbon.module.sys.model.request.TenantUserJoinRequest;
import cc.uncarbon.module.sys.model.request.TenantUserLookupRequest;
import cc.uncarbon.module.sys.model.valueobj.SysUserDTO;
import cc.uncarbon.module.sys.model.valueobj.TenantUserLookupVO;

import java.util.List;

/**
 * USER_FIRST 模式下，租户成员资格服务
 */
public interface SysUserFirstTenantService {

    /**
     * 全局找人（脱敏预览），可能返回多个候选人
     *
     * <p>账号精确匹配；完整手机号经全串盲索引精确匹配，同一手机号可对应多个账号，由调用方改用账号精确加入</p>
     */
    List<TenantUserLookupVO> lookupUsers(TenantUserLookupRequest request);

    /**
     * 将已有用户拉入本租户（可携带初始角色）
     */
    void joinTenant(TenantUserJoinRequest request);

    /**
     * 将成员移出本租户（清理本租户角色/部门关系并纠偏投影列）
     *
     * @return 被移出用户（全局口径，绕过行级过滤），供操作日志展示 pin
     */
    SysUserDTO expelTenantMember(TenantUserExpelRequest request);

}
