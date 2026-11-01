-- 行级多租户调优，仅在 isolation-strategy=LINE 部署时，于 init.sql 之后执行
BEGIN;

SET NAMES utf8mb4;

-- 日志时间范围分页：created_at 时间窗口覆盖全部租户
ALTER TABLE `sys_login_log`
  DROP INDEX `idx_sys_login_log_created_at`,
  ADD INDEX `idx_sys_login_log_tenant_created_at`(`tenant_id`, `created_at`) USING BTREE;

-- 登录失败取证按 user_pin 过滤：pin 跨租户重复（同一人多租户登录/失败尝试）
ALTER TABLE `sys_login_log`
  DROP INDEX `idx_sys_login_log_user_pin`,
  ADD INDEX `idx_sys_login_log_tenant_user_pin`(`tenant_id`, `user_pin`) USING BTREE;

ALTER TABLE `sys_operate_log`
  DROP INDEX `idx_sys_operate_log_created_at`,
  ADD INDEX `idx_sys_operate_log_tenant_created_at`(`tenant_id`, `created_at`) USING BTREE;

-- 存储点编码跨租户重复
ALTER TABLE `file_meta`
  DROP INDEX `idx_file_meta_storage_code`,
  ADD INDEX `idx_file_meta_tenant_storage_code`(`tenant_id`, `storage_code`) USING BTREE;

COMMIT;
