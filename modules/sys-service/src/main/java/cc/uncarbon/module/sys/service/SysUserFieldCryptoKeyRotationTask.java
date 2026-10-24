package cc.uncarbon.module.sys.service;

/**
 * @deprecated 已废弃（2026-10-23）：拆分迁移至 {@link SysUserFieldCryptoKeyRotationService} /
 *             {@link cc.uncarbon.module.sys.service.impl.SysUserFieldCryptoKeyRotationServiceImpl}，
 *             常规 DEK 轮换、索引根轮换与数据根泄露应急已聚合到新服务。本文件待删除。
 */
@Deprecated(forRemoval = true)
public final class SysUserFieldCryptoKeyRotationTask {

    private SysUserFieldCryptoKeyRotationTask() {
    }
}
