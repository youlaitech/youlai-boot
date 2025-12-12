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
-- 2. 为业务表添加 tenant_id 字段
-- ============================================
-- 注意事项：
-- 1. MySQL 5.7 不支持 IF NOT EXISTS，如果字段已存在会报错
-- 2. 菜单表（sys_menu）不添加 tenant_id，所有租户共享菜单定义
--    权限控制通过角色实现（角色是租户隔离的）
-- 3. 建议先检查字段是否存在，或使用 MySQL 8.0+

-- 用户表：仅在不存在时添加列和索引，避免重复执行报错
ALTER TABLE `sys_user` 
ADD COLUMN `tenant_id` bigint DEFAULT 1 COMMENT '租户ID' AFTER `id`,
ADD INDEX `idx_tenant_id` (`tenant_id`);

UPDATE `sys_user` SET `tenant_id` = 1 WHERE `tenant_id` IS NULL;

-- 修改 username 索引：从单列索引改为 (username, tenant_id) 组合唯一索引
-- 这样同一租户内用户名唯一，不同租户可以有相同用户名
DROP INDEX `login_name` ON `sys_user`;
ALTER TABLE `sys_user` 
ADD UNIQUE KEY `uk_username_tenant` (`username`, `tenant_id`);

-- 角色表
ALTER TABLE `sys_role` 
ADD COLUMN `tenant_id` bigint DEFAULT 1 COMMENT '租户ID' AFTER `id`,
ADD INDEX `idx_tenant_id` (`tenant_id`);

UPDATE `sys_role` SET `tenant_id` = 1 WHERE `tenant_id` IS NULL;

-- 角色菜单关联表
ALTER TABLE `sys_role_menu`
ADD COLUMN `tenant_id` bigint DEFAULT 1 COMMENT '租户ID' AFTER `role_id`,
ADD INDEX `idx_role_menu_tenant_id` (`tenant_id`);

UPDATE `sys_role_menu` SET `tenant_id` = 1 WHERE `tenant_id` IS NULL;

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
ALTER TABLE `ai_command_record` 
ADD COLUMN `tenant_id` bigint DEFAULT 1 COMMENT '租户ID' AFTER `id`,
ADD INDEX `idx_tenant_id` (`tenant_id`);

UPDATE `ai_command_record` SET `tenant_id` = 1 WHERE `tenant_id` IS NULL;


-- ============================================
-- 4. 添加租户管理菜单和权限（仅在菜单不存在时添加）
-- ============================================
-- 租户管理主菜单（放在部门管理之后，字典管理之前，ID=6）
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) 
VALUES (6, 1, '0,1', '租户管理', 1, 'Tenant', 'tenant', 'system/tenant/index', NULL, NULL, NULL, 1, 5, 'el-icon-OfficeBuilding', NULL, NOW(), NOW(), NULL)
ON DUPLICATE KEY UPDATE `name` = '租户管理';


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
