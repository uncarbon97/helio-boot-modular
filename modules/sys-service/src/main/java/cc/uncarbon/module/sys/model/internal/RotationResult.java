package cc.uncarbon.module.sys.model.internal;

public record RotationResult(
        // 是否跑完且复检通过
        boolean completed,
        // 完成的行数和批次数
        int processedRows, int processedBatches,
        // 失败时返回已处理到的记录与错误原因文本
        Long failedAfterId, String errorMessage) {
}
