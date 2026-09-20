-- ============================================================
-- 4.0.0 多租户登录模式（A/B 双模式）
-- 1. 新增用户-租户关联表 sys_user_tenant_relation（用户优先模式 USER_FIRST 专用；
--    租户优先模式 TENANT_FIRST 下不写此表，归属仍以 sys_user.tenant_id 单值为准）
-- 2. pin 唯一性与登录模式相关，索引二选一，切换模式时需重建：
--    租户优先模式（同 pin 跨租户共存）：
--      ALTER TABLE sys_user ADD UNIQUE KEY uk_tenant_pin (tenant_id, pin);
--      （若存在旧的全局 uk(pin)，需先删除）
--    用户优先模式（pin 全局唯一）：
--      ALTER TABLE sys_user ADD UNIQUE KEY uk_pin (pin);
-- 3. 用户优先模式下 sys_user.tenant_id 允许为 NULL（归属只看关联表）
-- ============================================================

CREATE TABLE IF NOT EXISTS `sys_user_tenant_relation`
(
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id`  BIGINT      NOT NULL COMMENT '租户ID',
    `user_id`    BIGINT      NOT NULL COMMENT '用户ID',
    `default_flag` TINYINT     NOT NULL DEFAULT 0 COMMENT '是否默认租户；1=是',
    `status`     TINYINT     NOT NULL DEFAULT 1 COMMENT '状态；1=启用 0=禁用',
    `created_at` TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时刻',
    `created_by` VARCHAR(64) NULL COMMENT '创建者',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_user` (`tenant_id`, `user_id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='系统用户-租户关联关系（用户优先模式 USER_FIRST 专用）';
