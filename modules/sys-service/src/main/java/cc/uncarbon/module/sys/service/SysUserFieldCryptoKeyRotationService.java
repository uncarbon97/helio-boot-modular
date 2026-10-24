package cc.uncarbon.module.sys.service;

import cc.uncarbon.module.sys.model.internal.OldKeyRemainingReport;
import cc.uncarbon.module.sys.model.internal.RootKeyRotationOptions;
import cc.uncarbon.module.sys.model.internal.RotationOptions;
import cc.uncarbon.module.sys.model.internal.RotationResult;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * sys_user 敏感字段加密密钥轮换服务
 *
 * <p>配套文档：{@code .agents/DB字段加密/密钥管理与轮换运维手册.md}。聚合两类密钥运维能力：</p>
 * <ul>
 *   <li>常规 DEK（keyId）轮换：零停机，存量密文前缀扫描重加密，见 {@link #rotateDataKey(String, RotationOptions)}；</li>
 *   <li>根密钥轮换：索引根密钥轮换（{@link #rotateIndexRootKey(RotationOptions)}，停写窗口内全量重算盲索引）
 *       与数据根密钥泄露应急（{@link #rotateDataRootKey(RootKeyRotationOptions)}，previous-root 兜底解密）。</li>
 * </ul>
 *
 * <p>多租户：所有方法均在租户忽略作用域内执行（{@code TenantContextHolder#callIgnored}），
 * 覆盖全租户 {@code sys_user}；DATASOURCE 数据源级隔离模式下动态数据源按租户路由，
 * 忽略作用域仅落默认库，<b>需逐租户数据源分别触发</b>。</p>
 *
 * <p>实现约定（三类轮换共用）：</p>
 * <ul>
 *   <li>走实体整行 {@code updateById}：密文列按活跃钥匙重加密，masked/盲索引派生列由框架自动回填，
 *       满足"一处赋值、四列齐变"；禁止 UpdateWrapper 局部更新密文列；</li>
 *   <li>明文仅存在于进程内存（TypeHandler 透明解密），不打印、不外发，日志只含 id 与计数；</li>
 *   <li>逻辑删除行不在处理范围（MyBatis-Plus 自动过滤）；删除旧 keyId / 应急收尾前需单独评估残留；</li>
 *   <li>单实例执行（多实例并发跑也幂等，只是浪费）；建议低峰期触发，批次间可配停顿限速；</li>
 *   <li>旧 keyId 含 {@code _} / {@code %} 时拒绝执行：二者是 LIKE 通配符，会造成误扫/漏扫。</li>
 * </ul>
 *
 * @author Uncarbon
 */
public interface SysUserFieldCryptoKeyRotationService {

    /**
     * 统计正在使用旧 keyId 的记录数
     *
     * @param oldKeyId 旧 keyId，如 k1
     * @return 两列各自命中行数 + 任一命中的总行数
     */
    OldKeyRemainingReport countOldKeyRemaining(String oldKeyId);

    /**
     * 判断能否安全地将旧 keyId 从 data-key-ids 配置中移除（旧 keyId 记录数 = 0）
     */
    boolean isSafeToDropOldKey(String oldKeyId);

    /**
     * 常规轮换：把存量旧 keyId 密文，使用当前活跃 keyId 重加密
     *
     * @param oldKeyId 旧 keyId，如 k1；不得等于当前活跃 keyId
     * @param options  批参数，null 走默认值
     */
    RotationResult rotateDataKey(String oldKeyId, @Nullable RotationOptions options);

    /**
     * 索引根密钥轮换：停写窗口内全量重算 masked/盲索引派生列
     *
     * <p>适用：索引根密钥长周期轮换，或索引根密钥泄露处置。数据根密钥与 keyId 集合不变，
     * 存量密文可正常解密，整行回写时密文列、派生列按新索引根密钥重算
     *
     * <p>前置人工完成：
     * <ol>
     *   <li>已停写（或摘除写流量节点）</li>
     *   <li>新索引根密钥已注入，且首节点已以新根密钥重启、
     *       {@code helium.db.field-crypto.canary.allow-overwrite=true} 完成首节点指纹覆写</li>
     *   <li>轮换完成立即回置 {@code allow-overwrite=false}，其余节点正常启动，canary 负责启动器对账</li>
     * </ol>
     *
     * @param options 批参数，null 走默认值
     */
    RotationResult rotateIndexRootKey(@Nullable RotationOptions options);

    /**
     * 数据根密钥泄露处置：新数据根（+必要时新索引根）+ 全新 keyId，全量重加密
     *
     * <p>前置人工完成：
     * <ol>
     *   <li>已停写（或摘除写流量节点）</li>
     *   <li>配置：新 dataRootKey（+必要时新 indexRootKey）
     *   <li>配置：新 previous-data-root-key + previous-data-key-ids
     *   <li>收尾：回置 allow-overwrite=false、移除 previous-* 配置，重启全节点，由 canary 对账</li>
     * </ol>
     *
     * @param options 批参数
     */
    RotationResult rotateDataRootKey(@NonNull RootKeyRotationOptions options);

}
