BEGIN;

SET NAMES utf8mb4;
-- 允许 id=0 落库：默认模式下 AUTO_INCREMENT 列遇 0 会改为自动生成
SET SESSION sql_mode = CONCAT(IFNULL(@@SESSION.sql_mode, ''), ',NO_AUTO_VALUE_ON_ZERO');
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `#数据表模板`;
CREATE TABLE `#数据表模板`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint(20) NULL DEFAULT 0 COMMENT '租户ID',
  `del_flag` smallint(6) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `created_at` timestamp NOT NULL COMMENT '创建时刻',
  `created_by` bigint(20) NULL DEFAULT NULL COMMENT '创建者',
  `updated_at` timestamp NULL DEFAULT NULL COMMENT '更新时刻',
  `updated_by` bigint(20) NULL DEFAULT NULL COMMENT '更新者',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据表模板' ROW_FORMAT = Dynamic;


DROP TABLE IF EXISTS `file_meta`;
CREATE TABLE `file_meta`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint(20) NULL DEFAULT 0 COMMENT '租户ID',
  `del_flag` tinyint(4) UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `created_at` timestamp NOT NULL COMMENT '创建时刻',
  `created_by` bigint(20) NULL DEFAULT NULL COMMENT '创建者',
  `updated_at` timestamp NULL DEFAULT NULL COMMENT '更新时刻',
  `updated_by` bigint(20) NULL DEFAULT NULL COMMENT '更新者',
  `storage_id` bigint(20) NOT NULL COMMENT '原始存储点ID',
  `storage_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '存储点编码',
  `storage_base_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '存储点主目录路径',
  `sub_dir_path` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '子目录路径',
  `storage_filename` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '存储文件名',
  `original_filename` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '原始文件名',
  `extend_name` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '扩展名',
  `file_size` bigint(20) NOT NULL COMMENT '文件大小',
  `digest_sha256` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'SHA256',
  `category` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件主分类',
  `direct_url` varchar(510) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '对象存储直链',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '文件元数据' ROW_FORMAT = Dynamic;


DROP TABLE IF EXISTS `file_storage`;
CREATE TABLE `file_storage`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint(20) NULL DEFAULT 0 COMMENT '租户ID',
  `del_flag` tinyint(4) UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `created_at` timestamp NOT NULL COMMENT '创建时刻',
  `created_by` bigint(20) NULL DEFAULT NULL COMMENT '创建者',
  `updated_at` timestamp NULL DEFAULT NULL COMMENT '更新时刻',
  `updated_by` bigint(20) NULL DEFAULT NULL COMMENT '更新者',
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '存储点编码',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '存储点名称',
  `platform_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '存储平台类型',
  `setting_json` varchar(3000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '{}' COMMENT '配置属性',
  `primary_flag` smallint(6) NOT NULL DEFAULT 0 COMMENT '主存储点标识',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '文件存储点' ROW_FORMAT = Dynamic;


DROP TABLE IF EXISTS `helium_field_crypto_canary`;
CREATE TABLE `helium_field_crypto_canary`  (
  `purpose` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用途标识',
  `fingerprint` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '指纹摘要',
  `updated_at` timestamp NOT NULL COMMENT '更新时刻',
  PRIMARY KEY (`purpose`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '根密钥指纹持久化' ROW_FORMAT = Dynamic;


DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint(20) NULL DEFAULT 0 COMMENT '租户ID',
  `del_flag` smallint(6) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `created_at` timestamp NOT NULL COMMENT '创建时刻',
  `created_by` bigint(20) NULL DEFAULT NULL COMMENT '创建者',
  `updated_at` timestamp NULL DEFAULT NULL COMMENT '更新时刻',
  `updated_by` bigint(20) NULL DEFAULT NULL COMMENT '更新者',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '部门名称',
  `parent_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '上级部门ID(根部门设置为0)',
  `sort` int(11) NOT NULL DEFAULT 1 COMMENT '排序',
  `status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '部门' ROW_FORMAT = Dynamic;


DROP TABLE IF EXISTS `sys_dict_category`;
CREATE TABLE `sys_dict_category`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint(20) NULL DEFAULT 0 COMMENT '租户ID',
  `del_flag` tinyint(4) UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `created_at` timestamp NOT NULL COMMENT '创建时刻',
  `created_by` bigint(20) NULL DEFAULT NULL COMMENT '创建者',
  `updated_at` timestamp NULL DEFAULT NULL COMMENT '更新时刻',
  `updated_by` bigint(20) NULL DEFAULT NULL COMMENT '更新者',
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典编码',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典名称',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '字典描述',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典分类' ROW_FORMAT = Dynamic;


DROP TABLE IF EXISTS `sys_dict_item`;
CREATE TABLE `sys_dict_item`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint(20) NULL DEFAULT 0 COMMENT '租户ID',
  `del_flag` tinyint(4) UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `created_at` timestamp NOT NULL COMMENT '创建时刻',
  `created_by` bigint(20) NULL DEFAULT NULL COMMENT '创建者',
  `updated_at` timestamp NULL DEFAULT NULL COMMENT '更新时刻',
  `updated_by` bigint(20) NULL DEFAULT NULL COMMENT '更新者',
  `category_id` bigint(20) NOT NULL COMMENT '所属分类ID',
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典项编码',
  `value` varchar(10000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典项值',
  `label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '字典项标签',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态',
  `sort` int(11) NOT NULL DEFAULT 1 COMMENT '排序',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '字典项描述',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典项' ROW_FORMAT = Dynamic;


DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint(20) NULL DEFAULT 0 COMMENT '租户ID',
  `del_flag` smallint(6) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `created_at` timestamp NOT NULL COMMENT '创建时刻',
  `created_by` bigint(20) NULL DEFAULT NULL COMMENT '创建者',
  `login_log_type` int(11) NOT NULL COMMENT '登录日志类型',
  `user_pin` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户账号',
  `user_id` bigint(20) NULL DEFAULT NULL COMMENT '用户ID',
  `user_type_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户类型编码',
  `visitor_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '来源IP地址',
  `visitor_user_agent` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '来源UA',
  `visitor_ip_location` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT 'IP地址归属地',
  `result_status` int(11) NOT NULL DEFAULT 0 COMMENT '结果状态',
  `failed_msg` varchar(3000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '失败原因文本',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统登录日志' ROW_FORMAT = Dynamic;


DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `del_flag` smallint(6) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `created_at` timestamp NOT NULL COMMENT '创建时刻',
  `created_by` bigint(20) NULL DEFAULT NULL COMMENT '创建者',
  `updated_at` timestamp NULL DEFAULT NULL COMMENT '更新时刻',
  `updated_by` bigint(20) NULL DEFAULT NULL COMMENT '更新者',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `parent_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '上级菜单ID(根菜单设置为0)',
  `menu_type` int(11) NOT NULL DEFAULT 1 COMMENT '菜单类型',
  `visible_scope` tinyint(4) NOT NULL DEFAULT 1 COMMENT '可见范围',
  `path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '路由地址',
  `permission` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '授权标识',
  `status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '状态',
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '图标',
  `sort` int(11) NULL DEFAULT 1 COMMENT '排序',
  `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '前端组件名称',
  `external_link` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '外链地址',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sys_menu_parent_id`(`parent_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 315 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统菜单' ROW_FORMAT = Dynamic;

INSERT INTO `sys_menu` VALUES (1, 0, '2026-10-01 00:00:00', 0, '2026-10-01 00:00:00', 0, '系统管理', 0, 0, 1, '/sys', '', 1, 'ant-design:setting-outlined', 100, NULL, '');
INSERT INTO `sys_menu` VALUES (2, 0, '2026-10-01 00:00:00', 0, '2026-10-01 00:00:00', 0, '文件管理', 0, 0, 1, '/file', '', 1, 'ant-design:cloud-outlined', 101, NULL, '');
INSERT INTO `sys_menu` VALUES (3, 0, '2026-10-01 00:00:00', 0, '2026-10-01 00:00:00', 0, '租户管理', 0, 0, 2, '/tenant', '', 1, 'ant-design:merge-outlined', 103, NULL, '');
INSERT INTO `sys_menu` VALUES (10, 0, '2026-10-01 00:00:00', NULL, '2026-10-01 00:00:00', 0, '用户管理', 1, 1, 1, '/sys/user', '', 1, 'ant-design:user-outlined', 100, 'sys/user/index', '');
INSERT INTO `sys_menu` VALUES (11, 0, '2026-10-01 00:00:00', NULL, '2026-10-01 00:00:00', 0, '角色管理', 1, 1, 1, '/sys/role', '', 1, 'ant-design:user-switch-outlined', 101, 'sys/role/index', '');
INSERT INTO `sys_menu` VALUES (12, 0, '2026-10-01 00:00:00', NULL, '2026-10-01 00:00:00', 0, '菜单管理', 1, 1, 2, '/sys/menu', '', 1, 'ant-design:appstore-filled', 102, 'sys/menu/index', '');
INSERT INTO `sys_menu` VALUES (13, 0, '2026-10-01 00:00:00', NULL, '2026-10-01 00:00:00', 0, '部门管理', 1, 1, 1, '/sys/dept', '', 1, 'ant-design:apartment-outlined', 103, '/sys/dept/index', '');
INSERT INTO `sys_menu` VALUES (14, 0, '2026-10-01 00:00:00', NULL, '2026-10-01 00:00:00', 0, '数据字典', 1, 1, 1, '/sys/dict', '', 1, 'ant-design:book-outlined', 104, '/sys/dict/index', '');
INSERT INTO `sys_menu` VALUES (15, 0, '2026-10-01 00:00:00', 0, '2026-10-01 00:00:00', 0, '审计日志', 1, 0, 1, '/audit-log', '', 1, 'ant-design:file-search-outlined', 105, NULL, '');
INSERT INTO `sys_menu` VALUES (16, 0, '2026-10-01 00:00:00', NULL, '2026-10-01 00:00:00', 0, '登录日志', 15, 1, 1, '/sys/login-log', '', 1, 'ant-design:file-sync-outlined', 100, 'sys/login-log/index', '');
INSERT INTO `sys_menu` VALUES (17, 0, '2026-10-01 00:00:00', NULL, '2026-10-01 00:00:00', 0, '操作日志', 15, 1, 1, '/sys/operate-log', '', 1, 'ant-design:file-text-outlined', 101, '/sys/operate-log/index', '');
INSERT INTO `sys_menu` VALUES (20, 0, '2026-10-01 00:00:00', NULL, '2026-10-01 00:00:00', 0, '文件存储点', 2, 1, 1, '/file/storage', '', 1, 'ant-design:cloud-server-outlined', 100, 'file/storage/index', '');
INSERT INTO `sys_menu` VALUES (21, 0, '2026-10-01 00:00:00', NULL, '2026-10-01 00:00:00', 0, '文件管理', 2, 1, 1, '/file/meta', '', 1, 'ant-design:cloud-upload-outlined', 101, 'file/meta/index', '');
INSERT INTO `sys_menu` VALUES (30, 0, '2026-10-01 00:00:00', NULL, '2026-10-01 00:00:00', 0, '租户套餐', 3, 1, 1, '/tenant/package', '', 1, 'ant-design:mac-command-outlined', 100, 'tenant/package/index', '');
INSERT INTO `sys_menu` VALUES (31, 0, '2026-10-01 00:00:00', NULL, '2026-10-01 00:00:00', 0, '租户管理', 3, 1, 1, '/tenant/meta', '', 1, 'ant-design:idcard-outlined', 101, 'tenant/meta/index', '');
INSERT INTO `sys_menu` VALUES (101, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '查询', 10, 2, 1, '', 'sys:user:read', 1, '', 1, NULL, '');
INSERT INTO `sys_menu` VALUES (102, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '新增', 10, 2, 1, '', 'sys:user:create', 1, '', 2, NULL, '');
INSERT INTO `sys_menu` VALUES (103, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '修改', 10, 2, 1, '', 'sys:user:update', 1, '', 3, NULL, '');
INSERT INTO `sys_menu` VALUES (104, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '删除', 10, 2, 1, '', 'sys:user:delete', 1, '', 4, NULL, '');
INSERT INTO `sys_menu` VALUES (105, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '分配角色', 10, 2, 1, '', 'sys:user:bind-role', 1, '', 4, NULL, '');
INSERT INTO `sys_menu` VALUES (106, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '修改部门', 10, 2, 1, '', 'sys:user:bind-dept', 1, '', 4, NULL, '');
INSERT INTO `sys_menu` VALUES (107, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '重置密码', 10, 2, 1, '', 'sys:user:reset-password', 1, '', 4, NULL, '');
INSERT INTO `sys_menu` VALUES (108, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '踢下线', 10, 2, 1, '', 'sys:user:kick-out', 1, '', 4, NULL, '');
INSERT INTO `sys_menu` VALUES (109, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '租户成员管理', 10, 2, 1, '', 'sys:user:join', 1, '', 5, NULL, '');
INSERT INTO `sys_menu` VALUES (111, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '查询', 11, 2, 1, '', 'sys:role:read', 1, '', 1, NULL, '');
INSERT INTO `sys_menu` VALUES (112, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '新增', 11, 2, 1, '', 'sys:role:create', 1, '', 2, NULL, '');
INSERT INTO `sys_menu` VALUES (113, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '修改', 11, 2, 1, '', 'sys:role:update', 1, '', 3, NULL, '');
INSERT INTO `sys_menu` VALUES (114, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '删除', 11, 2, 1, '', 'sys:role:delete', 1, '', 4, NULL, '');
INSERT INTO `sys_menu` VALUES (115, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '授权', 11, 2, 1, '', 'sys:role:bind-menu', 1, '', 4, NULL, '');
INSERT INTO `sys_menu` VALUES (121, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '查询', 12, 2, 1, '', 'sys:menu:read', 1, '', 1, NULL, '');
INSERT INTO `sys_menu` VALUES (122, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '新增', 12, 2, 1, '', 'sys:menu:create', 1, '', 2, NULL, '');
INSERT INTO `sys_menu` VALUES (123, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '修改', 12, 2, 1, '', 'sys:menu:update', 1, '', 3, NULL, '');
INSERT INTO `sys_menu` VALUES (124, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '删除', 12, 2, 1, '', 'sys:menu:delete', 1, '', 4, NULL, '');
INSERT INTO `sys_menu` VALUES (131, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '查询', 13, 2, 1, '', 'sys:dept:read', 1, '', 1, NULL, '');
INSERT INTO `sys_menu` VALUES (132, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '新增', 13, 2, 1, '', 'sys:dept:create', 1, '', 2, NULL, '');
INSERT INTO `sys_menu` VALUES (133, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '修改', 13, 2, 1, '', 'sys:dept:update', 1, '', 3, NULL, '');
INSERT INTO `sys_menu` VALUES (134, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '删除', 13, 2, 1, '', 'sys:dept:delete', 1, '', 4, NULL, '');
INSERT INTO `sys_menu` VALUES (141, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '查询', 14, 2, 1, '', 'sys:dict:read', 1, '', 1, NULL, '');
INSERT INTO `sys_menu` VALUES (142, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '新增', 14, 2, 1, '', 'sys:dict:create', 1, '', 2, NULL, '');
INSERT INTO `sys_menu` VALUES (143, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '修改', 14, 2, 1, '', 'sys:dict:update', 1, '', 3, NULL, '');
INSERT INTO `sys_menu` VALUES (144, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '删除', 14, 2, 1, '', 'sys:dict:delete', 1, '', 4, NULL, '');
INSERT INTO `sys_menu` VALUES (161, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '查询', 16, 2, 1, '', 'sys:login-log:read', 1, '', 1, NULL, '');
INSERT INTO `sys_menu` VALUES (171, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '查询', 17, 2, 1, '', 'sys:operate-log:read', 1, '', 1, NULL, '');
INSERT INTO `sys_menu` VALUES (201, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '查询', 20, 2, 1, '', 'file:storage:read', 1, '', 1, NULL, '');
INSERT INTO `sys_menu` VALUES (202, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '新增', 20, 2, 1, '', 'file:storage:create', 1, '', 2, NULL, '');
INSERT INTO `sys_menu` VALUES (203, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '修改', 20, 2, 1, '', 'file:storage:update', 1, '', 3, NULL, '');
INSERT INTO `sys_menu` VALUES (204, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '删除', 20, 2, 1, '', 'file:storage:delete', 1, '', 4, NULL, '');
INSERT INTO `sys_menu` VALUES (211, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '查询', 21, 2, 1, '', 'file:meta:read', 1, '', 1, NULL, '');
INSERT INTO `sys_menu` VALUES (212, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '新增', 21, 2, 1, '', 'file:meta:create', 1, '', 2, NULL, '');
INSERT INTO `sys_menu` VALUES (213, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '修改', 21, 2, 1, '', 'file:meta:update', 1, '', 3, NULL, '');
INSERT INTO `sys_menu` VALUES (214, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '删除', 21, 2, 1, '', 'file:meta:delete', 1, '', 4, NULL, '');
INSERT INTO `sys_menu` VALUES (301, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '查询', 30, 2, 1, '', 'tenant:package:read', 1, '', 1, NULL, '');
INSERT INTO `sys_menu` VALUES (302, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '新增', 30, 2, 1, '', 'tenant:package:create', 1, '', 2, NULL, '');
INSERT INTO `sys_menu` VALUES (303, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '修改', 30, 2, 1, '', 'tenant:package:update', 1, '', 3, NULL, '');
INSERT INTO `sys_menu` VALUES (304, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '删除', 30, 2, 1, '', 'tenant:package:delete', 1, '', 4, NULL, '');
INSERT INTO `sys_menu` VALUES (311, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '查询', 31, 2, 1, '', 'tenant:meta:read', 1, '', 1, NULL, '');
INSERT INTO `sys_menu` VALUES (312, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '新增', 31, 2, 1, '', 'tenant:meta:create', 1, '', 2, NULL, '');
INSERT INTO `sys_menu` VALUES (313, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '修改', 31, 2, 1, '', 'tenant:meta:update', 1, '', 3, NULL, '');
INSERT INTO `sys_menu` VALUES (314, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '删除', 31, 2, 1, '', 'tenant:meta:delete', 1, '', 4, NULL, '');

DROP TABLE IF EXISTS `sys_operate_log`;
CREATE TABLE `sys_operate_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint(20) NULL DEFAULT 0 COMMENT '租户ID',
  `del_flag` smallint(6) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `created_at` timestamp NOT NULL COMMENT '创建时刻',
  `created_by` bigint(20) NULL DEFAULT NULL COMMENT '创建者',
  `biz_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '业务类型',
  `behavior` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '行为',
  `biz_no` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '业务号',
  `operation` varchar(3000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作内容',
  `biz_extra` varchar(3000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '额外业务信息',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `user_type_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户类型编码',
  `request_method` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'HTTP请求方法',
  `request_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'HTTP请求路径',
  `visitor_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '来源IP地址',
  `visitor_user_agent` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '来源UA',
  `visitor_ip_location` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT 'IP地址归属地',
  `result_status` int(11) NOT NULL DEFAULT 0 COMMENT '结果状态',
  `failed_msg` varchar(3000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '失败原因文本',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统操作日志' ROW_FORMAT = Dynamic;


DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint(20) NULL DEFAULT 0 COMMENT '租户ID',
  `del_flag` smallint(6) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `created_at` timestamp NOT NULL COMMENT '创建时刻',
  `created_by` bigint(20) NULL DEFAULT NULL COMMENT '创建者',
  `updated_at` timestamp NULL DEFAULT NULL COMMENT '更新时刻',
  `updated_by` bigint(20) NULL DEFAULT NULL COMMENT '更新者',
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色编码',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '角色描述',
  `status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '状态',
  `flags` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '角色特殊标记',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统角色' ROW_FORMAT = Dynamic;

INSERT INTO `sys_role` VALUES (0, 0, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, 'SuperAdmin', '超级管理员', '', 1, 'builtin');

DROP TABLE IF EXISTS `sys_role_menu_relation`;
CREATE TABLE `sys_role_menu_relation`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint(20) NULL DEFAULT 0 COMMENT '租户ID',
  `created_at` timestamp NOT NULL COMMENT '创建时刻',
  `created_by` bigint(20) NULL DEFAULT NULL COMMENT '创建者',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `menu_id` bigint(20) NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk__tenant_role_menu`(`tenant_id`, `role_id`, `menu_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统角色-菜单关联关系' ROW_FORMAT = Dynamic;


DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0,
  `del_flag` smallint(6) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `created_at` timestamp NOT NULL COMMENT '创建时刻',
  `created_by` bigint(20) NULL DEFAULT NULL COMMENT '创建者',
  `updated_at` timestamp NULL DEFAULT NULL COMMENT '更新时刻',
  `updated_by` bigint(20) NULL DEFAULT NULL COMMENT '更新者',
  `pin` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '账号',
  `pwd` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
  `pwd_last_updated_at` timestamp NULL DEFAULT NULL COMMENT '密码最近一次更新时刻',
  `pwd_must_change` tinyint(4) NOT NULL DEFAULT 0 COMMENT '要求用户下次登录时修改密码',
  `nickname` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '昵称',
  `gender` tinyint(4) NOT NULL DEFAULT 0 COMMENT '性别',
  `status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '状态',
  `last_login_at` timestamp NULL DEFAULT NULL COMMENT '最后登录时刻',
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像URL',
  `phone_no` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `phone_no_masked` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号脱敏冗余',
  `phone_no_full_blind_index` char(24) CHARACTER SET ascii COLLATE ascii_bin NULL DEFAULT NULL COMMENT '手机号全串盲索引',
  `phone_no_tail4_blind_index` char(24) CHARACTER SET ascii COLLATE ascii_bin NULL DEFAULT NULL COMMENT '手机号后4位盲索引',
  `email` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '邮箱',
  `email_masked` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '邮箱脱敏冗余',
  `email_full_blind_index` char(24) CHARACTER SET ascii COLLATE ascii_bin NULL DEFAULT NULL COMMENT '邮箱全串盲索引',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统用户' ROW_FORMAT = Dynamic;

INSERT INTO `sys_user` VALUES (0, 0, 0, '2026-10-01 00:00:00', NULL, '2026-10-01 00:00:00', 0, 'admin', '$argon2id$v=19$m=19456,t=2,p=1$N5zOuCYckX+BVAFvVE4dQA$fidoxVwyp/UgRt9HtUCTLEO6WTp+yRWXiZH1vJQjL9M', '2026-10-01 00:00:00', 0, '超级管理员', 0, 1, '2026-10-01 00:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

DROP TABLE IF EXISTS `sys_user_dept_relation`;
CREATE TABLE `sys_user_dept_relation`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint(20) NULL DEFAULT 0 COMMENT '租户ID',
  `created_at` timestamp NOT NULL COMMENT '创建时刻',
  `created_by` bigint(20) NULL DEFAULT NULL COMMENT '创建者',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `dept_id` bigint(20) NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk__tenant_user_dept`(`tenant_id`, `user_id`, `dept_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统用户-部门关联关系' ROW_FORMAT = Dynamic;


DROP TABLE IF EXISTS `sys_user_role_relation`;
CREATE TABLE `sys_user_role_relation`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint(20) NULL DEFAULT 0 COMMENT '租户ID',
  `created_at` timestamp NOT NULL COMMENT '创建时刻',
  `created_by` bigint(20) NULL DEFAULT NULL COMMENT '创建者',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk__tenant_user_role`(`tenant_id`, `user_id`, `role_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统用户-角色关联关系' ROW_FORMAT = Dynamic;

INSERT INTO `sys_user_role_relation` VALUES (0, 0, '2026-10-01 00:00:00', NULL, 0, 0);

DROP TABLE IF EXISTS `sys_user_tenant_relation`;
CREATE TABLE `sys_user_tenant_relation`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint(20) NOT NULL COMMENT '租户ID',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时刻',
  `created_by` bigint(20) NULL DEFAULT NULL COMMENT '创建者',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_user`(`tenant_id`, `user_id`) USING BTREE,
  INDEX `idx_user`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统用户-租户关联关系' ROW_FORMAT = Dynamic;

INSERT INTO `sys_user_tenant_relation` VALUES (0, 0, '2026-10-01 00:00:00', NULL, 0);

DROP TABLE IF EXISTS `tenant_meta`;
CREATE TABLE `tenant_meta`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `del_flag` smallint(6) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `created_at` timestamp NOT NULL COMMENT '创建时刻',
  `created_by` bigint(20) NULL DEFAULT NULL COMMENT '创建者',
  `updated_at` timestamp NULL DEFAULT NULL COMMENT '更新时刻',
  `updated_by` bigint(20) NULL DEFAULT NULL COMMENT '更新者',
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '租户编码',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户名称',
  `status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '状态',
  `admin_user_id` bigint(20) NULL DEFAULT NULL COMMENT '租户管理员用户ID',
  `package_id` bigint(20) NULL DEFAULT NULL COMMENT '所属租户套餐ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '租户元数据' ROW_FORMAT = Dynamic;

INSERT INTO `tenant_meta` VALUES (0, 0, '2026-10-01 00:00:00', NULL, NULL, NULL, '0000', '平台自营域', 1, 0, NULL);

DROP TABLE IF EXISTS `tenant_package`;
CREATE TABLE `tenant_package`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `del_flag` smallint(6) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `created_at` timestamp NOT NULL COMMENT '创建时刻',
  `created_by` bigint(20) NULL DEFAULT NULL COMMENT '创建者',
  `updated_at` timestamp NULL DEFAULT NULL COMMENT '更新时刻',
  `updated_by` bigint(20) NULL DEFAULT NULL COMMENT '更新者',
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '套餐编码',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '套餐名称',
  `status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '状态',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '套餐描述',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '租户套餐' ROW_FORMAT = Dynamic;


DROP TABLE IF EXISTS `tenant_package_menu_relation`;
CREATE TABLE `tenant_package_menu_relation`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `created_at` timestamp NOT NULL COMMENT '创建时刻',
  `created_by` bigint(20) NULL DEFAULT NULL COMMENT '创建者',
  `package_id` bigint(20) NOT NULL COMMENT '租户套餐ID',
  `menu_id` bigint(20) NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 0 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '租户套餐-菜单关联关系' ROW_FORMAT = Dynamic;


SET FOREIGN_KEY_CHECKS = 1;

COMMIT;
