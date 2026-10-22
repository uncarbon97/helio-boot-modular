-- ============================================================================
-- 平台域种子脚本（§3.5 步骤 4 / 方案任务 2.9）：超管账号 + 内置角色 + 角色绑定
-- 用途：4.0.0 字段落库加密清库升级后重建平台域最小可用账号（tenant_id = 0）
--
-- 【执行前必读】
-- 1. pwd 列为 Argon2id PHC 格式哈希（PwdUtil 规格：m=19456,t=2,p=1，B64 无 padding）。
--    下方占位值不可登录：执行前请用工程内 PwdUtil.hash("你的初始口令") 生成并替换
--    （IDE 内运行该工具方法，或经部署流水线的初始化步骤生成）。
-- 2. must_change_password 置 1：超管首次登录强制改口令。
-- 3. 手机号/邮箱留空（NULL）：本脚本不写入 PII；需要时由超管登录后自助填写，
--    走应用写路径自动加密 + 派生列填充。
-- 4. sys_user_role_relation 若实际表结构含 tenant_id 列（行级租户过滤默认参与），保留 tenant_id=0；
--    无该列则删除对应字段（执行前核实表结构）。
-- 5. 状态值：status 1=启用（SysUserStatusEnum.ENABLED）；del_flag 0=未删除。
-- ============================================================================

-- 超管账号（平台域）
INSERT INTO sys_user (id, tenant_id, pin, pwd, pwd_last_updated_at, nickname, status,
                      gender, avatar_url, must_change_password, del_flag, created_at, created_by)
VALUES (0, 0, 'superadmin',
        '$argon2id$v=19$m=19456,t=2,p=1$REPLACE_WITH_PwdUtil.hash_OUTPUT$REPLACE_WITH_PwdUtil.hash_OUTPUT',
        NOW(), '超级管理员', 1, 0, NULL, 1, 0, NOW(), 0);

-- 内置超管角色（BUILTIN 标记，与代码 SysConstant.SUPER_ADMIN_ROLE_ID = 0 对应）
INSERT INTO sys_role (id, tenant_id, code, name, description, status, flags, del_flag, created_at, created_by)
VALUES (0, 0, 'SuperAdmin', '超级管理员', '平台内置超级管理员角色，不可手动分配或解除', 1, 'BUILTIN', 0, NOW(), 0);

-- 超管 - 角色绑定
INSERT INTO sys_user_role_relation (tenant_id, user_id, role_id, del_flag, created_at, created_by)
VALUES (0, 0, 0, 0, NOW(), 0);
