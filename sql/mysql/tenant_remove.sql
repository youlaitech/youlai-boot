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
-- 1. 删除用户租户关联表
-- ============================================
DROP TABLE IF EXISTS `sys_user_tenant`;

-- ============================================
-- 2. 删除租户表（可选）
-- ============================================
-- 注意：如果将来可能再次启用多租户，建议保留此表
-- 如需删除，取消下面的注释
-- DROP TABLE IF EXISTS `sys_tenant`;

-- ============================================
-- 3. 移除业务表的 tenant_id 字段和索引
-- ============================================
-- 注意：如果字段不存在会报错，请根据实际情况调整

-- 用户表
ALTER TABLE `sys_user` DROP INDEX IF EXISTS `idx_tenant_id`;
ALTER TABLE `sys_user` DROP COLUMN IF EXISTS `tenant_id`;

-- 角色表
ALTER TABLE `sys_role` DROP INDEX IF EXISTS `idx_tenant_id`;
ALTER TABLE `sys_role` DROP COLUMN IF EXISTS `tenant_id`;

-- 部门表
ALTER TABLE `sys_dept` DROP INDEX IF EXISTS `idx_tenant_id`;
ALTER TABLE `sys_dept` DROP COLUMN IF EXISTS `tenant_id`;

-- 通知公告表
ALTER TABLE `sys_notice` DROP INDEX IF EXISTS `idx_tenant_id`;
ALTER TABLE `sys_notice` DROP COLUMN IF EXISTS `tenant_id`;

-- 系统日志表
ALTER TABLE `sys_log` DROP INDEX IF EXISTS `idx_tenant_id`;
ALTER TABLE `sys_log` DROP COLUMN IF EXISTS `tenant_id`;

-- AI 命令记录表
ALTER TABLE `ai_command_log` DROP INDEX IF EXISTS `idx_tenant_id`;
ALTER TABLE `ai_command_log` DROP COLUMN IF EXISTS `tenant_id`;

-- 代码生成配置表（如果存在）
-- ALTER TABLE `gen_config` DROP INDEX IF EXISTS `idx_tenant_id`;
-- ALTER TABLE `gen_config` DROP COLUMN IF EXISTS `tenant_id`;

-- 代码生成字段配置表（如果存在）
-- ALTER TABLE `gen_field_config` DROP INDEX IF EXISTS `idx_tenant_id`;
-- ALTER TABLE `gen_field_config` DROP COLUMN IF EXISTS `tenant_id`;

-- 菜单表（如果之前添加了）
-- ALTER TABLE `sys_menu` DROP INDEX IF EXISTS `idx_tenant_id`;
-- ALTER TABLE `sys_menu` DROP COLUMN IF EXISTS `tenant_id`;

-- ============================================
-- 4. 删除租户管理菜单和权限
-- ============================================
-- 删除角色菜单关联
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (6, 141, 142, 143, 144, 145);

-- 删除租户管理权限按钮
DELETE FROM `sys_menu` WHERE `id` IN (141, 142, 143, 144, 145);

-- 删除租户管理主菜单
DELETE FROM `sys_menu` WHERE `id` = 6;

-- 恢复字典管理的排序（从7改回6）
UPDATE `sys_menu` SET `sort` = 6 WHERE `id` = 7 AND `sort` = 7;

-- 恢复字典项的排序（从8改回7）
UPDATE `sys_menu` SET `sort` = 7 WHERE `id` = 8 AND `sort` = 8;

-- 恢复系统日志的排序（从9改回8）
UPDATE `sys_menu` SET `sort` = 8 WHERE `id` = 9 AND `sort` = 9;

-- 恢复系统配置的排序（从10改回9）
UPDATE `sys_menu` SET `sort` = 9 WHERE `id` = 10 AND `sort` = 10;

-- 恢复通知公告的排序（从11改回10）
UPDATE `sys_menu` SET `sort` = 10 WHERE `id` = 11 AND `sort` = 11;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 脚本执行完成
-- ============================================
-- 执行完成后，请执行以下操作：
-- 1. 在 application.yml 中配置：
--    youlai:
--      tenant:
--        enabled: false
-- 2. 更新 BaseEntity.java，将 tenantId 字段的 exist 设置为 false
--    或移除 tenantId 字段（如果确定不再使用）
-- ============================================
-- 注意：
-- 1. MySQL 5.7 不支持 IF EXISTS 语法，如果执行报错，请手动检查字段是否存在
-- 2. 对于 MySQL 8.0+，可以使用上面的语法
-- 3. 如果使用 MySQL 5.7，请先检查字段是否存在，再执行删除操作
-- ============================================
