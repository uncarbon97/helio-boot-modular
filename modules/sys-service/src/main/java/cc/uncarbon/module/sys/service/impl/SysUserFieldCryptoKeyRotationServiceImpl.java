package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.db.fieldcrypto.blindindex.BlindIndexCalculator;
import cc.uncarbon.framework.helium.db.fieldcrypto.blindindex.BlindIndexValueExtractors;
import cc.uncarbon.framework.helium.db.fieldcrypto.constant.FieldCryptoConstant;
import cc.uncarbon.framework.helium.db.fieldcrypto.field.FieldCipher;
import cc.uncarbon.framework.helium.db.fieldcrypto.field.FieldCipherContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.module.commons.constant.SQLSegment;
import cc.uncarbon.module.sys.constant.SysUserCipherCols;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.model.internal.OldKeyRemainingReport;
import cc.uncarbon.module.sys.model.internal.RootKeyRotationOptions;
import cc.uncarbon.module.sys.model.internal.RotationOptions;
import cc.uncarbon.module.sys.model.internal.RotationResult;
import cc.uncarbon.module.sys.service.SysUserFieldCryptoKeyRotationService;
import cn.hutool.core.thread.ThreadUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

/**
 * sys_user 敏感字段加密密钥轮换服务实现
 *
 * @author Uncarbon
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysUserFieldCryptoKeyRotationServiceImpl implements SysUserFieldCryptoKeyRotationService {

    private static final Pattern KEY_ID_PATTERN = FieldCryptoConstant.KEY_ID_PATTERN;

    /**
     * 抽样校验失败明细最多保留条数，避免异常场景刷爆日志/返回体
     */
    private static final int MAX_SAMPLE_MISMATCH_LOGS = 10;

    /**
     * 密文列前缀匹配专用 apply 裸 SQL 模板（入参 = keyId 前缀 + "%"）
     *
     * <p>不能用 lambda {@code likeRight}：实体 @TableField(typeHandler) 映射会被 MP
     * {@code AbstractLambdaWrapper#columnToMapping} 拼进参数占位符，LIKE 条件值先被加密成密文再去匹配，
     * 恒 0 命中（残留计数假清零、轮换闸门失效）。apply 的 {0} 占位符不带列 mapping，参数原样绑定
     */
    private static final String PHONE_NO_LIKE_PREFIX_SQL = "phone_no LIKE {0}";
    private static final String EMAIL_LIKE_PREFIX_SQL = "email LIKE {0}";

    private final SysUserMapper sysUserMapper;
    private final BlindIndexCalculator blindIndexCalculator;


    @Override
    public OldKeyRemainingReport countOldKeyRemaining(String oldKeyId) {
        return runTenantIgnored(() -> doCountOldKeyRows(requireOldKeyId(oldKeyId)));
    }

    @Override
    public boolean isSafeToDropOldKey(String oldKeyId) {
        return countOldKeyRemaining(oldKeyId).allClear();
    }

    @Override
    public RotationResult rotateDataKey(String oldKeyId, RotationOptions options) {
        String validOldKeyId = requireOldKeyId(oldKeyId);
        RotationOptions opts = options == null ? RotationOptions.DEFAULTS : options;
        return runTenantIgnored(() -> doRotateDataKey(validOldKeyId, opts));
    }

    @Override
    public RotationResult rotateIndexRootKey(@Nullable RotationOptions options) {
        RotationOptions opts = options == null ? RotationOptions.DEFAULTS : options;
        return runTenantIgnored(() -> doRotateIndexRootKey(opts));
    }

    @Override
    public RotationResult rotateDataRootKey(@NonNull RootKeyRotationOptions options) {
        final String LOG_PREFIX = "[数据根轮换]";
        if (options.oldKeyIds() == null || options.oldKeyIds().isEmpty()) {
            throw new IllegalArgumentException(LOG_PREFIX + " oldKeyIds 不能为空");
        }
        List<String> oldKeyIds = options.oldKeyIds().stream()
                .map(SysUserFieldCryptoKeyRotationServiceImpl::requireOldKeyId)
                .distinct()
                .toList();

        // 本节点必须已配置 previous-data-root-key + previous-data-key-ids，否则旧密文解不开
        FieldCipher cipher = FieldCipherContext.getRequired();
        String activeKeyId = cipher.getActiveKeyId();
        for (String oldKeyId : oldKeyIds) {
            if (oldKeyId.equals(activeKeyId)) {
                throw new IllegalArgumentException(
                        LOG_PREFIX + " 旧 keyId 不能等于当前活跃 keyId（active=" + activeKeyId + "）: " + oldKeyId);
            }
            if (!cipher.canDecrypt(oldKeyId)) {
                throw new IllegalStateException(
                        LOG_PREFIX + " 本节点未配置 previous-data-root-key / previous-data-key-ids，"
                                + "无法兜底解密旧代密文: " + oldKeyId);
            }
        }

        RotationOptions opts = options.batch() == null ? RotationOptions.DEFAULTS : options.batch();
        return runTenantIgnored(() -> doRotateDataRootKey(oldKeyIds, opts));
    }

    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    /**
     * 在租户忽略作用域内执行系统级全表操作，作用域结束自动恢复，不污染调用方上下文
     */
    private <T> T runTenantIgnored(Callable<T> op) {
        try {
            return TenantContextHolder.callIgnored(op);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("租户忽略作用域内执行失败", e);
        }
    }

    private static String requireOldKeyId(String oldKeyId) {
        if (oldKeyId == null || !KEY_ID_PATTERN.matcher(oldKeyId).matches()) {
            throw new IllegalArgumentException("旧 keyId 不合法（限 [A-Za-z0-9-]{1,16}，且不得含 LIKE 通配符 _ %）: " + oldKeyId);
        }
        return oldKeyId;
    }

    private OldKeyRemainingReport doCountOldKeyRows(String oldKeyId) {
        final String LOG_PREFIX = "[数据 keyId 轮换]";
        String prefix = oldKeyId + "$";

        long phoneRows = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserEntity>()
                .apply(PHONE_NO_LIKE_PREFIX_SQL, prefix + "%"));
        long emailRows = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserEntity>()
                .apply(EMAIL_LIKE_PREFIX_SQL, prefix + "%"));
        long rowsTotal = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserEntity>()
                .and(w -> w.apply(PHONE_NO_LIKE_PREFIX_SQL, prefix + "%")
                        .or().apply(EMAIL_LIKE_PREFIX_SQL, prefix + "%")));

        OldKeyRemainingReport report = new OldKeyRemainingReport(oldKeyId, phoneRows, emailRows, rowsTotal);
        log.info(LOG_PREFIX + " >> 旧 keyId 残留计数: {}", report);
        return report;
    }

    private RotationResult doRotateDataKey(String oldKeyId, RotationOptions opts) {
        final String LOG_PREFIX = "[数据 keyId 轮换]";
        String prefix = oldKeyId + "$";

        String activeKeyId = FieldCipherContext.getRequired().getActiveKeyId();
        if (oldKeyId.equals(activeKeyId)) {
            throw new IllegalArgumentException(
                    LOG_PREFIX + " 旧 keyId 不能等于当前活跃 keyId（active=" + activeKeyId + "）");
        }

        long lastId = -1L;
        int processedRows = 0;
        int processedBatches = 0;
        try {
            while (true) {
                List<SysUserEntity> batch = sysUserMapper.selectList(new LambdaQueryWrapper<SysUserEntity>()
                        .gt(SysUserEntity::getId, lastId)
                        .and(w -> w.apply(PHONE_NO_LIKE_PREFIX_SQL, prefix + "%")
                                .or().apply(EMAIL_LIKE_PREFIX_SQL, prefix + "%"))
                        .orderByAsc(SysUserEntity::getId)
                        .last(SQLSegment.LIMIT + opts.batchSize()));
                if (batch.isEmpty()) {
                    break;
                }

                for (SysUserEntity row : batch) {
                    // 整行回写：密文列按活跃钥匙重加密，派生列由框架回填；行内容不变、明文不出内存
                    sysUserMapper.updateById(row);
                }

                lastId = batch.getLast().getId();
                processedRows += batch.size();
                processedBatches++;
                if (processedBatches % 10 == 0) {
                    log.info(LOG_PREFIX + " >> 进行中: 已重加密 {} 行 / {} 批, 游标 id={}", processedRows, processedBatches, lastId);
                }

                if (opts.pauseMillisBetweenBatches() > 0) {
                    ThreadUtil.safeSleep(opts.pauseMillisBetweenBatches());
                }
            }
        } catch (Exception e) {
            log.error(LOG_PREFIX + " >> 批处理失败, 已重加密 {} 行 / {} 批, 失败游标 id={}", processedRows, processedBatches, lastId, e);
            return new RotationResult(false, processedRows, processedBatches, lastId, e.toString());
        }

        OldKeyRemainingReport remaining = doCountOldKeyRows(oldKeyId);
        log.info(LOG_PREFIX + " >> 存量重加密完成: {} 行 / {} 批; 复扫残留: {}", processedRows, processedBatches, remaining);
        return new RotationResult(remaining.allClear(), processedRows, processedBatches, lastId,
                remaining.allClear() ? null : "完成扫描后仍残留旧密文: " + remaining);
    }

    /**
     * 数据根密钥应急轮换
     */
    private RotationResult doRotateDataRootKey(List<String> oldKeyIds, RotationOptions opts) {
        final String LOG_PREFIX = "[数据根密钥轮换]";
        List<String> prefixes = oldKeyIds.stream().map(keyId -> keyId + "$").toList();

        long lastId = -1L;
        int processedRows = 0;
        int processedBatches = 0;
        List<String> sampleMismatches = new ArrayList<>();
        try {
            while (true) {
                // 任一旧前缀命中即入选：phone/email 两列 × 旧前缀全组合平铺 OR
                List<SysUserEntity> batch = sysUserMapper.selectList(new LambdaQueryWrapper<SysUserEntity>()
                        .gt(SysUserEntity::getId, lastId)
                        .and(w -> {
                            for (int i = 0; i < prefixes.size(); i++) {
                                if (i > 0) {
                                    w.or();
                                }
                                w.apply(PHONE_NO_LIKE_PREFIX_SQL, prefixes.get(i) + "%")
                                        .or()
                                        .apply(EMAIL_LIKE_PREFIX_SQL, prefixes.get(i) + "%");
                            }
                        })
                        .orderByAsc(SysUserEntity::getId)
                        .last(SQLSegment.LIMIT + opts.batchSize()));
                if (batch.isEmpty()) {
                    break;
                }

                for (SysUserEntity row : batch) {
                    // 整行回写
                    sysUserMapper.updateById(row);
                }

                // 复检：每批首行抽样校验
                String mismatch = verifyBlindIndexesSample(batch.getFirst().getId());
                if (mismatch != null && sampleMismatches.size() < MAX_SAMPLE_MISMATCH_LOGS) {
                    sampleMismatches.add(mismatch);
                }

                lastId = batch.get(batch.size() - 1).getId();
                processedRows += batch.size();
                processedBatches++;
                if (processedBatches % 10 == 0) {
                    log.info(LOG_PREFIX + " >> 进行中: 已重加密 {} 行 / {} 批, 游标 id={}", processedRows, processedBatches, lastId);
                }

                if (opts.pauseMillisBetweenBatches() > 0) {
                    ThreadUtil.safeSleep(opts.pauseMillisBetweenBatches());
                }
            }
        } catch (Exception e) {
            log.error(LOG_PREFIX + " >> 批处理失败, 已重加密 {} 行 / {} 批, 失败游标 id={}", processedRows, processedBatches, lastId, e);
            return new RotationResult(false, processedRows, processedBatches, lastId, e.toString());
        }

        List<OldKeyRemainingReport> remainingList = oldKeyIds.stream().map(this::doCountOldKeyRows).toList();
        boolean allClear = sampleMismatches.isEmpty() && remainingList.stream().allMatch(OldKeyRemainingReport::allClear);
        log.info(LOG_PREFIX + " >> 全量重加密完成: {} 行 / {} 批; 复扫残留: {}; 抽样校验: {}",
                processedRows, processedBatches, remainingList, allClear ? "通过" : "不一致 " + sampleMismatches);
        return new RotationResult(allClear, processedRows, processedBatches, lastId,
                allClear ? null : "残留旧密文 " + remainingList + " 或抽样校验不一致 " + sampleMismatches);
    }

    private RotationResult doRotateIndexRootKey(RotationOptions opts) {
        final String LOG_PREFIX = "[盲索引根密钥轮换]";
        long lastId = -1L;
        int processedRows = 0;
        int processedBatches = 0;
        List<String> sampleMismatches = new ArrayList<>();
        try {
            while (true) {
                // 无前缀过滤：索引值不带代际标记，只能全量扫；重跑仅重写、不损坏
                List<SysUserEntity> batch = sysUserMapper.selectList(new LambdaQueryWrapper<SysUserEntity>()
                        .gt(SysUserEntity::getId, lastId)
                        .orderByAsc(SysUserEntity::getId)
                        .last(SQLSegment.LIMIT + opts.batchSize()));
                if (batch.isEmpty()) {
                    break;
                }

                for (SysUserEntity row : batch) {
                    // 整行回写
                    sysUserMapper.updateById(row);
                }

                // 每批首行复查抽样校验
                String mismatch = verifyBlindIndexesSample(batch.getFirst().getId());
                if (mismatch != null && sampleMismatches.size() < MAX_SAMPLE_MISMATCH_LOGS) {
                    sampleMismatches.add(mismatch);
                }

                lastId = batch.getLast().getId();
                processedRows += batch.size();
                processedBatches++;
                if (processedBatches % 10 == 0) {
                    log.info(LOG_PREFIX + " >> 进行中: 已重算 {} 行 / {} 批, 游标 id={}", processedRows, processedBatches, lastId);
                }

                if (opts.pauseMillisBetweenBatches() > 0) {
                    ThreadUtil.safeSleep(opts.pauseMillisBetweenBatches());
                }
            }
        } catch (Exception e) {
            log.error(LOG_PREFIX + " >> 批处理失败, 已重算 {} 行 / {} 批, 失败游标 id={}", processedRows, processedBatches, lastId, e);
            return new RotationResult(false, processedRows, processedBatches, lastId, e.toString());
        }

        boolean allClear = sampleMismatches.isEmpty();
        log.info(LOG_PREFIX + " >> 全量重算完成: {} 行 / {} 批; 抽样校验: {}", processedRows, processedBatches,
                allClear ? "通过" : "不一致 " + sampleMismatches);
        return new RotationResult(allClear, processedRows, processedBatches, lastId,
                allClear ? null : "抽样校验盲索引不一致: " + sampleMismatches);
    }

    /**
     * 抽样校验
     * {@link SysUserEntity}
     *
     * @return null = 校验通过；否则携带 id 与不一致列描述（不含明文）
     */
    private String verifyBlindIndexesSample(Long id) {
        SysUserEntity row = sysUserMapper.selectById(id);
        if (row == null) {
            return "id=" + id + " 复查行不存在";
        }

        List<String> bad = new ArrayList<>(3);
        if (row.getPhoneNo() != null) {
            String expectFull = blindIndexCalculator.compute(SysUserCipherCols.PHONE_FULL_PURPOSE,
                    new BlindIndexValueExtractors.CleanTrim().extract(row.getPhoneNo()));
            if (!Objects.equals(expectFull, row.getPhoneNoFullBlindIndex())) {
                bad.add(SysUserCipherCols.PHONE_FULL_PURPOSE);
            }
            String expectTail4 = blindIndexCalculator.compute(SysUserCipherCols.PHONE_TAIL4_PURPOSE,
                    new SysUserEntity.Tail4Digits().extract(row.getPhoneNo()));
            if (!Objects.equals(expectTail4, row.getPhoneNoTail4BlindIndex())) {
                bad.add(SysUserCipherCols.PHONE_TAIL4_PURPOSE);
            }
        }
        if (row.getEmail() != null) {
            String expectEmail = blindIndexCalculator.compute(SysUserCipherCols.EMAIL_FULL_PURPOSE,
                    new BlindIndexValueExtractors.LowerTrim().extract(row.getEmail()));
            if (!Objects.equals(expectEmail, row.getEmailFullBlindIndex())) {
                bad.add(SysUserCipherCols.EMAIL_FULL_PURPOSE);
            }
        }
        return bad.isEmpty() ? null : "id=" + id + " 盲索引不一致列: " + bad;
    }
}
