package cc.uncarbon.module.sys.model.internal;

public record RotationOptions(
        // 每批行数
        int batchSize,
        // 批间停顿（0 = 不限速）
        long pauseMillisBetweenBatches) {

    public static final RotationOptions DEFAULTS = new RotationOptions(200, 200);

    public RotationOptions {
        if (batchSize < 1 || batchSize > 1000) {
            throw new IllegalArgumentException("batchSize 须在 1~1000: " + batchSize);
        }
        if (pauseMillisBetweenBatches < 0) {
            throw new IllegalArgumentException("pauseMillisBetweenBatches 不得为负: " + pauseMillisBetweenBatches);
        }
    }
}
