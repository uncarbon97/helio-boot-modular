package cc.uncarbon.module.sys.model.internal;

import java.util.List;

/**
 * 数据根泄露应急批参数：oldKeyIds 旧 keyId 集合（前缀扫描范围）；batch 批参数
 */
public record RootKeyRotationOptions(List<String> oldKeyIds,
                                     RotationOptions batch) {
}
