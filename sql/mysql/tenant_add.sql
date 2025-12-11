-- ============================================
-- 多租户支持 SQL 脚本（为现有系统添加多租户功能）
-- ============================================
-- 说明：此脚本用于为现有表添加 tenant_id 字段，启用多租户功能
-- 适用场景：已有系统需要升级支持多租户
-- 执行前请确保已备份数据库！
-- ============================================

USE youlai_admin;

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1. 创建租户表（如果不存在）
-- ============================================
DROP TABLE IF EXISTS `sys_tenant`;
CREATE TABLE `sys_tenant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '租户ID',
  `name` varchar(100) NOT NULL COMMENT '租户名称',
  `code` varchar(50) NOT NULL COMMENT '租户编码（唯一）',
  `contact_name` varchar(50) DEFAULT NULL COMMENT '联系人姓名',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系人电话',
  `contact_email` varchar(100) DEFAULT NULL COMMENT '联系人邮箱',
  `domain` varchar(100) DEFAULT NULL COMMENT '租户域名（用于域名识别）',
  `logo` varchar(255) DEFAULT NULL COMMENT '租户Logo',
  `status` tinyint DEFAULT '1' COMMENT '状态(1-正常 0-禁用)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间（NULL表示永不过期）',
  `create_time` datetime COMMENT '创建时间',
  `update_time` datetime COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  UNIQUE KEY `uk_domain` (`domain`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='系统租户表';

-- 插入默认租户
INSERT INTO `sys_tenant` (`id`, `name`, `code`, `status`, `create_time`) VALUES 
(1, '默认租户', 'DEFAULT', 1, NOW());

-- ============================================
-- 2. 创建租户切换审计日志表
-- ============================================
DROP TABLE IF EXISTS `sys_tenant_switch_log`;
CREATE TABLE `sys_tenant_switch_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `username` varchar(64) COMMENT '用户名',
  `from_tenant_id` bigint COMMENT '原租户ID',
  `from_tenant_name` varchar(100) COMMENT '原租户名称',
  `to_tenant_id` bigint NOT NULL COMMENT '目标租户ID',
  `to_tenant_name` varchar(100) COMMENT '目标租户名称',
  `switch_time` datetime NOT NULL COMMENT '切换时间',
  `ip_address` varchar(50) COMMENT 'IP地址',
  `user_agent` varchar(500) COMMENT '浏览器信息',
  `status` tinyint DEFAULT '1' COMMENT '切换状态（1-成功 0-失败）',
  `fail_reason` varchar(255) COMMENT '失败原因',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_switch_time` (`switch_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户切换审计日志表';

-- ============================================
-- 3. 创建用户租户关联表（支持一个用户属于多个租户）
-- ============================================
DROP TABLE IF EXISTS `sys_user_tenant`;
CREATE TABLE `sys_user_tenant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `is_default` tinyint DEFAULT '0' COMMENT '是否默认租户(1-是 0-否)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_tenant` (`user_id`, `tenant_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='用户租户关联表（多租户模式）';

-- ============================================
-- 3. 为业务表添加 tenant_id 字段
-- ============================================
-- 注意：MySQL 5.7 不支持 IF NOT EXISTS，如果字段已存在会报错
-- 建议先检查字段是否存在，或使用 MySQL 8.0+

-- 用户表
ALTER TABLE `sys_user` 
ADD COLUMN `tenant_id` bigint DEFAULT 1 COMMENT '租户ID' AFTER `id`,
ADD INDEX `idx_tenant_id` (`tenant_id`);

-- 更新现有数据的 tenant_id（设置为默认租户）
UPDATE `sys_user` SET `tenant_id` = 1 WHERE `tenant_id` IS NULL;

-- 角色表
ALTER TABLE `sys_role` 
ADD COLUMN `tenant_id` bigint DEFAULT 1 COMMENT '租户ID' AFTER `id`,
ADD INDEX `idx_tenant_id` (`tenant_id`);

UPDATE `sys_role` SET `tenant_id` = 1 WHERE `tenant_id` IS NULL;

-- 部门表
ALTER TABLE `sys_dept` 
ADD COLUMN `tenant_id` bigint DEFAULT 1 COMMENT '租户ID' AFTER `id`,
ADD INDEX `idx_tenant_id` (`tenant_id`);

UPDATE `sys_dept` SET `tenant_id` = 1 WHERE `tenant_id` IS NULL;

-- 通知公告表
ALTER TABLE `sys_notice` 
ADD COLUMN `tenant_id` bigint DEFAULT 1 COMMENT '租户ID' AFTER `id`,
ADD INDEX `idx_tenant_id` (`tenant_id`);

UPDATE `sys_notice` SET `tenant_id` = 1 WHERE `tenant_id` IS NULL;

-- 系统日志表
ALTER TABLE `sys_log` 
ADD COLUMN `tenant_id` bigint DEFAULT 1 COMMENT '租户ID' AFTER `id`,
ADD INDEX `idx_tenant_id` (`tenant_id`);

UPDATE `sys_log` SET `tenant_id` = 1 WHERE `tenant_id` IS NULL;

-- AI 命令记录表
ALTER TABLE `ai_command_log` 
ADD COLUMN `tenant_id` bigint DEFAULT 1 COMMENT '租户ID' AFTER `id`,
ADD INDEX `idx_tenant_id` (`tenant_id`);

UPDATE `ai_command_log` SET `tenant_id` = 1 WHERE `tenant_id` IS NULL;

-- 代码生成配置表（如果存在）
-- ALTER TABLE `gen_config` 
-- ADD COLUMN `tenant_id` bigint DEFAULT 1 COMMENT '租户ID' AFTER `id`,
-- ADD INDEX `idx_tenant_id` (`tenant_id`);
-- UPDATE `gen_config` SET `tenant_id` = 1 WHERE `tenant_id` IS NULL;

-- 代码生成字段配置表（如果存在）
-- ALTER TABLE `gen_field_config` 
-- ADD COLUMN `tenant_id` bigint DEFAULT 1 COMMENT '租户ID' AFTER `id`,
-- ADD INDEX `idx_tenant_id` (`tenant_id`);
-- UPDATE `gen_field_config` SET `tenant_id` = 1 WHERE `tenant_id` IS NULL;

-- ============================================
-- 4. 初始化现有用户的租户关联（默认租户）
-- ============================================
INSERT INTO `sys_user_tenant` (`user_id`, `tenant_id`, `is_default`) 
SELECT `id`, 1, 1 FROM `sys_user` WHERE `is_deleted` = 0
ON DUPLICATE KEY UPDATE `is_default` = 1;

-- ============================================
-- 5. 添加租户管理菜单和权限（仅在菜单不存在时添加）
-- ============================================
-- 租户管理主菜单（放在部门管理之后，字典管理之前，ID=6）
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) 
VALUES (6, 1, '0,1', '租户管理', 1, 'Tenant', 'tenant', 'system/tenant/index', NULL, NULL, NULL, 1, 5, 'el-icon-OfficeBuilding', NULL, NOW(), NOW(), NULL)
ON DUPLICATE KEY UPDATE `name` = '租户管理';

-- 调整字典管理的排序（从6改为7）
UPDATE `sys_menu` SET `sort` = 7 WHERE `id` = 7 AND `sort` = 6;

-- 调整字典项的排序（从7改为8）
UPDATE `sys_menu` SET `sort` = 8 WHERE `id` = 8 AND `sort` = 7;

-- 调整系统日志的排序（从8改为9）
UPDATE `sys_menu` SET `sort` = 9 WHERE `id` = 9 AND `sort` = 8;

-- 调整系统配置的排序（从9改为10）
UPDATE `sys_menu` SET `sort` = 10 WHERE `id` = 10 AND `sort` = 9;

-- 调整通知公告的排序（从10改为11）
UPDATE `sys_menu` SET `sort` = 11 WHERE `id` = 11 AND `sort` = 10;

-- 租户管理权限按钮（ID: 141-145）
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) 
VALUES 
(141, 6, '0,1,6', '租户查询', 4, NULL, '', NULL, 'sys:tenant:query', NULL, NULL, 1, 1, '', NULL, NOW(), NOW(), NULL),
(142, 6, '0,1,6', '租户新增', 4, NULL, '', NULL, 'sys:tenant:add', NULL, NULL, 1, 2, '', NULL, NOW(), NOW(), NULL),
(143, 6, '0,1,6', '租户编辑', 4, NULL, '', NULL, 'sys:tenant:edit', NULL, NULL, 1, 3, '', NULL, NOW(), NOW(), NULL),
(144, 6, '0,1,6', '租户删除', 4, NULL, '', NULL, 'sys:tenant:delete', NULL, NULL, 1, 4, '', NULL, NOW(), NOW(), NULL),
(145, 6, '0,1,6', '租户启用/禁用', 4, NULL, '', NULL, 'sys:tenant:status', NULL, NULL, 1, 5, '', NULL, NOW(), NOW(), NULL)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- 为系统管理员角色（role_id=2）分配租户管理菜单权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) 
VALUES 
(2, 6),
(2, 141),
(2, 142),
(2, 143),
(2, 144),
(2, 145)
ON DUPLICATE KEY UPDATE `role_id` = VALUES(`role_id`);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 脚本执行完成
-- ============================================
-- 执行完成后，请在 application.yml 中配置：
-- youlai:
--   tenant:
--     enabled: true
--     column: tenant_id
--     default-tenant-id: 1
--     header-name: tenant-id
--     ignore-tables:
--       - sys_tenant
--       - sys_dict
--       - sys_dict_item
--       - sys_config
-- ============================================
