-- ============================================
-- 多租户移除脚本（移除多租户功能）
-- ============================================
-- 说明：此脚本用于移除多租户功能，删除 tenant_id 字段和相关表
-- 适用场景：不再需要多租户功能，需要回退到单租户模式
-- 执行前请确保已备份数据库！
-- 警告：此操作不可逆，请谨慎执行！
-- ============================================

USE youlai_admin;

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1. 删除租户表（可选）
-- ============================================
-- 注意：如果将来可能再次启用多租户，建议保留此表
-- 如需删除，取消下面的注释
-- DROP TABLE IF EXISTS `sys_tenant`;

-- ============================================
-- 2. 移除业务表的 tenant_id 字段和索引
-- ============================================
-- 注意：如果字段不存在会报错，请根据实际情况调整

-- 用户表
-- 先删除组合唯一索引
ALTER TABLE `sys_user` DROP INDEX `uk_username_tenant`;
-- 删除租户ID索引和字段
ALTER TABLE `sys_user` DROP INDEX `idx_tenant_id`;
ALTER TABLE `sys_user` DROP COLUMN `tenant_id`;
-- 恢复原来的用户名唯一索引
ALTER TABLE `sys_user` ADD UNIQUE KEY `login_name` (`username`);

-- 角色表
ALTER TABLE `sys_role` DROP INDEX `idx_tenant_id`;
ALTER TABLE `sys_role` DROP COLUMN `tenant_id`;

-- 角色菜单关联表
ALTER TABLE `sys_role_menu` DROP INDEX `idx_role_menu_tenant_id`;
ALTER TABLE `sys_role_menu` DROP COLUMN `tenant_id`;

-- 部门表
ALTER TABLE `sys_dept` DROP INDEX `idx_tenant_id`;
ALTER TABLE `sys_dept` DROP COLUMN `tenant_id`;

-- 通知公告表
ALTER TABLE `sys_notice` DROP INDEX `idx_tenant_id`;
ALTER TABLE `sys_notice` DROP COLUMN `tenant_id`;

-- 系统日志表
ALTER TABLE `sys_log` DROP INDEX `idx_tenant_id`;
ALTER TABLE `sys_log` DROP COLUMN `tenant_id`;

-- AI 命令记录表
ALTER TABLE `ai_command_record` DROP INDEX `idx_tenant_id`;
ALTER TABLE `ai_command_record` DROP COLUMN `tenant_id`;

-- ============================================
-- 3. 删除租户管理菜单和权限
-- ============================================
-- 删除角色菜单关联
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (6, 141, 142, 143, 144, 145);

-- 删除租户管理权限按钮
DELETE FROM `sys_menu` WHERE `id` IN (141, 142, 143, 144, 145);

-- 删除租户管理主菜单
DELETE FROM `sys_menu` WHERE `id` = 6;

SET FOREIGN_KEY_CHECKS = 1;