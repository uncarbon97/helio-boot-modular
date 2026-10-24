package cc.uncarbon.module.sys.model.internal;

/**
 * 残留计数报告
 */
public record OldKeyRemainingReport(String oldKeyId, long phoneColumnRows, long emailColumnRows, long rowsTotal) {

    public boolean allClear() {
        return rowsTotal == 0;
    }
}
