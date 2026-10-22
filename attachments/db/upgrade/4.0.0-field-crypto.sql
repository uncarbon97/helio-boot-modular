-- ============================================================================
-- 4.0.0 字段落库加密（PII）：sys_user 手机号/邮箱 密文 + 脱敏冗余列 + 盲索引列 + canary 元数据表
-- 方案：.agents/2026-10-19/字段落库加密方案.MD（六审定稿：phone/email 零 UNIQUE、零查重、零"已被使用"错误码）
--
-- 【升级前置：已建 4.0.0-rc 内部库必须先执行 §3.5 清库步骤，本节为其唯一权威引用】
--   1. 备份（含明文列，仅应急回读用，不进常规备份链）：
--      备份文件加密存放，口令双人分段保管（任一人无法单独解密）；
--      升级验证通过后 7 日内销毁，运维记录登记销毁时间与执行人（§3.5 / §5.1 escrow）。
--   2. 清空用户域数据：sys_user 及关联表 sys_user_role_relation、sys_user_dept_relation、sys_user_tenant_relation。
--   3. 执行本文件 DDL（DROP 明文列即明文永久丢失；新装库不创建明文列）。
--   4. 超管重建：执行 ../seed/platform-domain-seed.sql（超管 sys_user 行 tenant_id=0、内置角色、角色绑定）。
--   5. 验证：sys_user 行数 = 重建账号数；新账号三派生列 NULL 对齐；三个普通索引存在、无任何 UNIQUE 索引。
--
-- 密钥配置：bootstrap/src/main/resources/configs/helium-all.yml（helium.crypto.field.*）
-- 运维红线：改密文列必须同步派生列；AAD/索引 purpose/base64 变体为冻结参数，禁止原地变更（§3.1）
-- ============================================================================

-- ============ MySQL（主数据源） ============
-- 三审：回填 tenant_id NULL 行为平台租户 0 后收紧非空（多租户数据卫生前提，§1.1）；
-- 示例类型按 BIGINT，执行前请核实实际列定义
UPDATE sys_user SET tenant_id = 0 WHERE tenant_id IS NULL;

ALTER TABLE sys_user
  MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 0,
  DROP COLUMN phone_no,
  DROP COLUMN email,
  ADD COLUMN phone_no_encrypted         VARCHAR(512) NULL COMMENT '手机号密文 {keyId}$base64(nonce||ct||tag)',
  ADD COLUMN phone_no_masked            VARCHAR(32)  NULL COMMENT '手机号脱敏冗余列，如 189****8927',
  ADD COLUMN phone_no_blind_index       CHAR(24) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '手机号全串盲索引 base64(HMAC-SHA256截断128bit)，等值定位',
  ADD COLUMN phone_no_tail_blind_index  CHAR(24) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '手机号尾8位盲索引(仅检索,允许碰撞)',
  ADD COLUMN email_encrypted            VARCHAR(512) NULL COMMENT '邮箱密文',
  ADD COLUMN email_masked               VARCHAR(255) NULL COMMENT '邮箱脱敏冗余列，如 al***@example.com',
  ADD COLUMN email_blind_index          CHAR(24) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '邮箱全串盲索引(小写归一化)，等值定位';

-- 五审/六审：全部普通索引，无唯一约束（同租户同手机号/同邮箱多用户均为合法态）
CREATE INDEX idx_sys_user_tenant_phone ON sys_user (tenant_id, phone_no_blind_index);
CREATE INDEX idx_sys_user_phone_tail   ON sys_user (phone_no_tail_blind_index);
CREATE INDEX idx_sys_user_tenant_email ON sys_user (tenant_id, email_blind_index);

-- canary 元数据表（四审，§5.3 步骤 6）：根密钥双指纹（field-ik / kek-data），
-- 框架首启幂等写入、此后每次启动比对，任一不符即拒启；非业务表，不进租户路由
CREATE TABLE IF NOT EXISTS helium_field_crypto_canary (
  purpose     VARCHAR(32) PRIMARY KEY COMMENT 'field-ik / kek-data',
  fingerprint CHAR(64) NOT NULL COMMENT 'SHA-256(canary 向量输出) hex，只存摘要不存密钥',
  gmt_update  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '字段落库加密根密钥 canary，漂移即拒启';

-- ============ PostgreSQL（按所用数据源执行，与 MySQL 二选一） ============
-- ALTER TABLE sys_user
--   DROP COLUMN phone_no,
--   DROP COLUMN email,
--   ADD COLUMN phone_no_encrypted         VARCHAR(512),
--   ADD COLUMN phone_no_masked            VARCHAR(32),
--   ADD COLUMN phone_no_blind_index       CHAR(24),
--   ADD COLUMN phone_no_tail_blind_index  CHAR(24),
--   ADD COLUMN email_encrypted            VARCHAR(512),
--   ADD COLUMN email_masked               VARCHAR(255),
--   ADD COLUMN email_blind_index          CHAR(24);
--
-- ALTER TABLE sys_user ALTER COLUMN tenant_id SET NOT NULL;  -- 先回填 0 再收紧
--
-- COMMENT ON COLUMN sys_user.phone_no_encrypted         IS '手机号密文 {keyId}$base64(nonce||ct||tag)';
-- COMMENT ON COLUMN sys_user.phone_no_masked            IS '手机号脱敏冗余列';
-- COMMENT ON COLUMN sys_user.phone_no_blind_index       IS '手机号全串盲索引 base64(HMAC-SHA256截断128bit)';
-- COMMENT ON COLUMN sys_user.phone_no_tail_blind_index  IS '手机号尾8位盲索引(仅检索,允许碰撞)';
-- COMMENT ON COLUMN sys_user.email_encrypted            IS '邮箱密文';
-- COMMENT ON COLUMN sys_user.email_masked               IS '邮箱脱敏冗余列';
-- COMMENT ON COLUMN sys_user.email_blind_index          IS '邮箱全串盲索引(小写归一化)';
--
-- CREATE INDEX idx_sys_user_tenant_phone ON sys_user (tenant_id, phone_no_blind_index);
-- CREATE INDEX idx_sys_user_phone_tail   ON sys_user (phone_no_tail_blind_index);
-- CREATE INDEX idx_sys_user_tenant_email ON sys_user (tenant_id, email_blind_index);
--
-- -- canary 元数据表（PG）
-- CREATE TABLE IF NOT EXISTS helium_field_crypto_canary (
--   purpose     VARCHAR(32) PRIMARY KEY,
--   fingerprint CHAR(64) NOT NULL,
--   gmt_update  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
-- );
-- COMMENT ON TABLE helium_field_crypto_canary IS '根密钥 canary 双指纹（field-ik / kek-data），漂移即拒启';
