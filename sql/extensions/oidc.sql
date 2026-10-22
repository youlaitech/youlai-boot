-- 统一认证（OIDC）扩展：菜单、权限与演示账号绑定
-- 需先执行 sql/youlai-admin.sql；本脚本手动执行，不随应用启动
-- 菜单 ID：模块顶级 92；补在基础菜单下的按钮取父 ID 加 90 起（101 → 10190）

-- ----------------------------
-- 应用中心（顶级菜单，全员可见）
-- ----------------------------
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES
 (92, 0, '0', '应用中心', 'M', 'AppCenter', '/apps', 'workspace/apps/index', NULL, 1, 1, 10, 'el-icon-Grid', NULL, now(), now(), NULL);

-- ----------------------------
-- 用户管理页的账号绑定按钮（挂在基础菜单 101 下）
-- ----------------------------
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES
 (10190, 101, '0,1,101', '绑定查询', 'B', NULL, '', NULL, 'sso:binding:list',   NULL, 1, 8,  '', NULL, now(), now(), NULL),
 (10191, 101, '0,1,101', '新增绑定', 'B', NULL, '', NULL, 'sso:binding:create', NULL, 1, 9,  '', NULL, now(), now(), NULL),
 (10192, 101, '0,1,101', '改绑',     'B', NULL, '', NULL, 'sso:binding:rebind', NULL, 1, 10, '', NULL, now(), now(), NULL),
 (10193, 101, '0,1,101', '解除绑定', 'B', NULL, '', NULL, 'sso:binding:delete', NULL, 1, 11, '', NULL, now(), now(), NULL);

-- ----------------------------
-- 角色授权
-- 应用中心全员可见（基础角色 2-7；ROOT 由代码全量放行）
-- 账号绑定按钮仅系统管理员（role_id=2）
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` VALUES (2, 92), (3, 92), (4, 92), (5, 92), (6, 92), (7, 92);
INSERT IGNORE INTO `sys_role_menu` VALUES (2, 10190), (2, 10191), (2, 10192), (2, 10193);

-- ----------------------------
-- 演示账号绑定：认证中心 youlai realm 的 demo@youlai.tech 绑到本地 youlai 账号
-- external_id 是认证中心签发的 sub，重建该用户或换 realm 后会变，届时按新 ID Token 的 sub 改这一行
-- ----------------------------
INSERT IGNORE INTO `sys_user_identity` (`platform`, `issuer`, `external_id`, `user_id`, `bind_method`, `create_time`, `update_time`)
VALUES ('OIDC', 'https://auth.youlai.tech/realms/youlai', '33c85d2f-6c6c-40e1-a8ec-85612afc2bf5', 1, 'IMPORT', now(), now());
