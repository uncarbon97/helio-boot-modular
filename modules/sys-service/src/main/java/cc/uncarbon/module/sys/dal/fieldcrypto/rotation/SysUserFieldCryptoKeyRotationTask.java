package cc.uncarbon.module.sys.dal.fieldcrypto.rotation;

import cc.uncarbon.framework.helium.db.fieldcrypto.field.FieldCipherContext;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.regex.Pattern;

/**
 * sys_user 数据加密 keyId 轮换
 *
 * <p>配套文档：{@code .agents/2026-10-23/密钥轮换SOP.MD} §3。执行前提：SOP-A 阶段 1（全员 data-key-ids: [k1, k2]）
 * 与阶段 2（active-data-key-id = k2）均已完成，本任务负责把存量 {@code k1$} 密文重加密为活跃钥匙：</p>
 *
 * <ul>
 *   <li>{@link #countOldKeyRows(String)} / {@link #isSafeToRemoveOldKey(String)}：删除旧 keyId 前的硬闸门
 *       ——{@code LIKE 'k1$%'} 计数 = 0 是唯一标准，时间观察期只是必要条件；</li>
 *   <li>{@link #doRotation(String, RotationOptions)}：存量重加密，按 id 游标分批，幂等可续跑
 *       （重加密后前缀变 {@code k2$}，自动脱离扫描范围）。</li>
 * </ul>
 *
 * <p>实现约定：</p>
 * <ul>
 *   <li>走实体整行 {@code updateById}：TypeHandler 用活跃钥匙重加密，masked/盲索引派生列由框架自动回填，
 *       满足"一处赋值、四列齐变"；禁止改为 UpdateWrapper 局部更新密文列；</li>
 *   <li>明文仅存在于进程内存（TypeHandler 透明解密），本类不打印、不外发明文，日志只含 id 与计数；</li>
 *   <li>逻辑删除行不在处理范围（MyBatis-Plus 自动过滤，计数与重加密口径一致）；若业务支持"恢复已删用户"，
 *       删除旧 keyId 前需单独评估这部分残留 {@code k1$} 行；</li>
 *   <li>单实例执行（多实例并发跑也幂等，只是浪费）；建议低峰期触发，批次间可配停顿限速；</li>
 *   <li>旧 keyId 含 {@code _} / {@code %} 时拒绝执行：二者是 LIKE 通配符，会造成误扫/漏扫。
 *       keyId 选名请避开（框架字符集 [A-Za-z0-9_-] 允许，但本任务的 LIKE 扫描不允许）。</li>
 * </ul>
 *
 * @author Uncarbon
 */
@Slf4j
//@Component
@RequiredArgsConstructor
public class SysUserFieldCryptoKeyRotationTask {

    /**
     * 与框架 FieldCryptoConstant.KEY_ID_PATTERN 一致，仅用于入参快速校验
     */
    private static final Pattern KEY_ID_PATTERN = Pattern.compile("^[A-Za-z0-9-]{1,16}$");

    private final SysUserMapper sysUserMapper;

    private static String requireOldKeyId(String oldKeyId) {
        if (oldKeyId == null || !KEY_ID_PATTERN.matcher(oldKeyId).matches()) {
            throw new IllegalArgumentException("[密钥轮换] 旧 keyId 不合法（限 [A-Za-z0-9-]{1,16}，且不得含 LIKE 通配符 _ %）: " + oldKeyId);
        }
        return oldKeyId;
    }

    /**
     * 旧 keyId 残留计数。只做原始列值 LIKE 统计，不触发解密。
     *
     * @param oldKeyId 旧 keyId，如 k1
     * @return 两列各自命中行数 + 任一命中的总行数
     */
    public OldKeyRemainingReport countOldKeyRows(String oldKeyId) {
        String prefix = requireOldKeyId(oldKeyId) + "$";

        long phoneRows = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserEntity>()
                .likeRight(SysUserEntity::getPhoneNo, prefix));
        long emailRows = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserEntity>()
                .likeRight(SysUserEntity::getEmail, prefix));
        long rowsTotal = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserEntity>()
                .and(w -> w.likeRight(SysUserEntity::getPhoneNo, prefix)
                        .or().likeRight(SysUserEntity::getEmail, prefix)));

        OldKeyRemainingReport report = new OldKeyRemainingReport(oldKeyId, phoneRows, emailRows, rowsTotal);
        log.info("[密钥轮换] >> 旧 keyId 残留计数: {}", report);
        return report;
    }

    /**
     * 是否允许把旧 keyId 从 data-key-ids 配置中移除（旧 keyId 记录 = 0）
     */
    public boolean isSafeToRemoveOldKey(String oldKeyId) {
        return countOldKeyRows(oldKeyId).allClear();
    }


    /*
     * ------------------------------------------------------------------
     * 参数与结果载体
     * ------------------------------------------------------------------
     */

    /**
     * 把存量旧 keyId 密文重加密为当前活跃钥匙，游标分批、幂等可续跑、失败即停。
     *
     * @param oldKeyId 旧 keyId，如 k1；不得等于当前活跃 keyId
     * @param options  批参数
     * @return 执行结果；completed=false 时 errorMessage 携带失败点，修复后直接重跑即可续
     */
    public RotationResult doRotation(String oldKeyId, RotationOptions options) {
        String prefix = requireOldKeyId(oldKeyId) + "$";
        RotationOptions opts = options == null ? RotationOptions.DEFAULTS : options;

        String activeKeyId = FieldCipherContext.getRequired().getActiveKeyId();
        if (oldKeyId.equals(activeKeyId)) {
            throw new IllegalArgumentException(
                    "[密钥轮换] 旧 keyId 不能等于当前活跃 keyId（active=" + activeKeyId
                            + "），先完成 SOP-A 阶段 2 再执行重加密");
        }

        long lastId = 0L;
        int processedRows = 0;
        int processedBatches = 0;
        try {
            while (true) {
                List<SysUserEntity> batch = sysUserMapper.selectList(new LambdaQueryWrapper<SysUserEntity>()
                        .gt(SysUserEntity::getId, lastId)
                        .and(w -> w.likeRight(SysUserEntity::getPhoneNo, prefix)
                                .or().likeRight(SysUserEntity::getEmail, prefix))
                        .orderByAsc(SysUserEntity::getId)
                        .last("LIMIT " + opts.batchSize()));
                if (batch.isEmpty()) {
                    break;
                }

                for (SysUserEntity row : batch) {
                    // 整行回写：密文列按活跃钥匙重加密，派生列由框架回填；行内容不变、明文不出内存
                    sysUserMapper.updateById(row);
                }

                lastId = batch.get(batch.size() - 1).getId();
                processedRows += batch.size();
                processedBatches++;
                if (processedBatches % 10 == 0) {
                    log.info("[密钥轮换] >> 进行中: 已重加密 {} 行 / {} 批, 游标 id={}", processedRows, processedBatches, lastId);
                }

                if (opts.pauseMillisBetweenBatches() > 0) {
                    Thread.sleep(opts.pauseMillisBetweenBatches());
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("[密钥轮换] >> 被中断: 已重加密 {} 行 / {} 批, 游标 id={}", processedRows, processedBatches, lastId);
            return new RotationResult(false, processedRows, processedBatches, lastId, "interrupted");
        } catch (Exception e) {
            log.error("[密钥轮换] >> 批处理失败, 已重加密 {} 行 / {} 批, 失败游标 id={}", processedRows, processedBatches, lastId, e);
            return new RotationResult(false, processedRows, processedBatches, lastId, e.toString());
        }

        OldKeyRemainingReport remaining = countOldKeyRows(oldKeyId);
        log.info("[密钥轮换] >> 存量重加密完成: {} 行 / {} 批; 复扫残留: {}", processedRows, processedBatches, remaining);
        return new RotationResult(remaining.allClear(), processedRows, processedBatches, lastId,
                remaining.allClear() ? null : "完成扫描后仍残留旧密文: " + remaining);
    }

    /**
     * 批参数：batchSize 每批行数；pauseMillisBetweenBatches 批间停顿（0 = 不限速）
     */
    public record RotationOptions(int batchSize, long pauseMillisBetweenBatches) {

        public static final RotationOptions DEFAULTS = new RotationOptions(200, 200);

        public RotationOptions {
            if (batchSize < 1 || batchSize > 1000) {
                throw new IllegalArgumentException("[密钥轮换] batchSize 须在 1~1000: " + batchSize);
            }
            if (pauseMillisBetweenBatches < 0) {
                throw new IllegalArgumentException("[密钥轮换] pauseMillisBetweenBatches 不得为负: " + pauseMillisBetweenBatches);
            }
        }
    }

    /**
     * 残留计数报告
     */
    public record OldKeyRemainingReport(String oldKeyId, long phoneColumnRows, long emailColumnRows, long rowsTotal) {

        public boolean allClear() {
            return rowsTotal == 0;
        }
    }

    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    /**
     * 执行结果：completed = 全程跑完且复扫残留为 0；失败时携带 failedAfterId 游标与 errorMessage
     */
    public record RotationResult(boolean completed, int processedRows, int processedBatches,
                                 Long failedAfterId, String errorMessage) {
    }
}
