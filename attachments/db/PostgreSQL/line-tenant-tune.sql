-- 行级多租户调优
BEGIN;

-- 日志时间范围分页：created_at 时间窗口覆盖全部租户
DROP INDEX IF EXISTS idx_sys_login_log_created_at;
CREATE INDEX idx_sys_login_log_tenant_created_at ON sys_login_log (tenant_id, created_at);

-- 登录失败取证按 user_pin 过滤：pin 跨租户重复（同一人多租户登录/失败尝试）
DROP INDEX IF EXISTS idx_sys_login_log_user_pin;
CREATE INDEX idx_sys_login_log_tenant_user_pin ON sys_login_log (tenant_id, user_pin);

DROP INDEX IF EXISTS idx_sys_operate_log_created_at;
CREATE INDEX idx_sys_operate_log_tenant_created_at ON sys_operate_log (tenant_id, created_at);

-- 存储点编码跨租户重复
DROP INDEX IF EXISTS idx_file_meta_storage_code;
CREATE INDEX idx_file_meta_tenant_storage_code ON file_meta (tenant_id, storage_code);

COMMIT;
