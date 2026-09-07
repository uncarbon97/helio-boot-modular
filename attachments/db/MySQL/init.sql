-- =====================================================================
-- helium-monolith 4.0.0 增量 DDL (MySQL)
-- =====================================================================

-- 系统用户表：记录密码最近一次更新时刻
ALTER TABLE `sys_user`
    ADD COLUMN `pwd_updated_at` datetime NULL DEFAULT NULL COMMENT '密码最近一次更新时刻' AFTER `pwd_salt`;
