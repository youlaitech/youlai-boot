-- 统一认证扩展：应用中心入口与演示身份关联。
-- 先执行 sql/youlai-admin.sql；provider 必须与 security.oidc.provider 一致。
-- 身份绑定由用户在个人中心自助完成，不设管理权限：开关由 security.oidc 控制。

INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES
 (92, 0, '0', '应用中心', 'M', 'AppCenter', '/apps', 'workspace/apps/index', NULL, 1, 1, 10, 'el-icon-Grid', NULL, now(), now(), NULL);

-- 身份绑定改为个人中心自助，清理早期脚本建立的管理员代绑权限。
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (10190, 10191, 10192, 10193);
DELETE FROM `sys_menu` WHERE `id` IN (10190, 10191, 10192, 10193);

-- 应用中心沿用演示角色授权。
INSERT IGNORE INTO `sys_role_menu` VALUES (2, 92), (3, 92), (4, 92), (5, 92), (6, 92), (7, 92);

-- 保留演示关联：当前演示认证中心签发的 sub 对应本地 youlai 用户。
-- issuer 在 security.oidc 中配置；更换认证中心或重建外部用户后按实际 sub 更新。
INSERT IGNORE INTO `sys_user_identity`
    (`provider`, `provider_account_id`, `user_id`, `display_name`, `create_time`, `update_time`)
VALUES ('oidc-main', '33c85d2f-6c6c-40e1-a8ec-85612afc2bf5', 1, 'demo@youlai.tech', now(), now());
