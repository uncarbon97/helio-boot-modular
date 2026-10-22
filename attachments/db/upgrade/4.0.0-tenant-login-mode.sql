-- ------------------------------------------------------------
-- 4. 用户-租户关联表（唯一真源；default_flag 已砍 R11，首选租户记忆走 sys_user.tenant_id）
-- ------------------------------------------------------------

-- ------------------------------------------------------------
-- 6. 回填后验证（人工核对，全部应为 0 行 / 空）
-- ------------------------------------------------------------

-- 6.1 参与隔离的表不应再有 NULL 租户
SELECT 'sys_user' AS tbl, COUNT(*) AS null_tenant_rows FROM `sys_user` WHERE `tenant_id` IS NULL
UNION ALL SELECT 'sys_role', COUNT(*) FROM `sys_role` WHERE `tenant_id` IS NULL
UNION ALL SELECT 'sys_dept', COUNT(*) FROM `sys_dept` WHERE `tenant_id` IS NULL
UNION ALL SELECT 'sys_user_role_relation', COUNT(*) FROM `sys_user_role_relation` WHERE `tenant_id` IS NULL
UNION ALL SELECT 'sys_role_menu_relation', COUNT(*) FROM `sys_role_menu_relation` WHERE `tenant_id` IS NULL
UNION ALL SELECT 'sys_user_dept_relation', COUNT(*) FROM `sys_user_dept_relation` WHERE `tenant_id` IS NULL;

-- 6.2 投影列与真源表一致性（对账；有结果即漂移）
SELECT u.`id` AS user_id, u.`tenant_id` AS projected, rel.`tenant_id` AS relation_tenant
FROM `sys_user` u
LEFT JOIN `sys_user_tenant_relation` rel
       ON rel.`user_id` = u.`id`
WHERE rel.`id` IS NULL OR rel.`tenant_id` != u.`tenant_id`;

-- 6.3 同租户重复 pin（应仅出现在历史脏数据；建键失败时先清理这里）
SELECT `tenant_id`, `pin`, COUNT(*) AS cnt
FROM `sys_user`
GROUP BY `tenant_id`, `pin`
HAVING COUNT(*) > 1;
