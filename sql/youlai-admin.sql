
# YouLai_Admin 数据库(MySQL 5.7 ~ MySQL 8.x)
# Copyright (c) 2021-present, youlai.tech


-- ----------------------------
-- 1. 创建数据库
-- ----------------------------
CREATE DATABASE IF NOT EXISTS youlai_admin CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;


-- ----------------------------
-- 2. 创建表 && 数据初始化
-- ----------------------------
USE youlai_admin;

SET NAMES utf8mb4;  # 设置字符集
SET FOREIGN_KEY_CHECKS = 0; # 关闭外键检查，加快导入速度

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`  (
                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                             `name` varchar(100) NOT NULL COMMENT '部门名称',
                             `code` varchar(100) NOT NULL COMMENT '部门编号',
                             `parent_id` bigint DEFAULT 0 COMMENT '父节点id',
                             `tree_path` varchar(255) NOT NULL COMMENT '父节点id路径',
                             `sort` smallint DEFAULT 0 COMMENT '显示顺序',
                             `status` tinyint DEFAULT 1 COMMENT '状态(1-正常 0-禁用)',
                             `create_by` bigint NULL COMMENT '创建人ID',
                             `create_time` datetime NULL COMMENT '创建时间',
                             `update_by` bigint NULL COMMENT '修改人ID',
                             `update_time` datetime NULL COMMENT '更新时间',
                             `is_deleted` tinyint DEFAULT 0 COMMENT '逻辑删除标识(1-已删除 0-未删除)',
                             PRIMARY KEY (`id`) USING BTREE,
                             UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE COMMENT '部门编号唯一索引'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '部门管理表';

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
INSERT INTO `sys_dept` VALUES (1, '有来技术', 'YOULAI', 0, '0', 1, 1, 1, NULL, 1, now(), 0);
INSERT INTO `sys_dept` VALUES (2, '研发部门', 'RD001', 1, '0,1', 1, 1, 2, NULL, 2, now(), 0);
INSERT INTO `sys_dept` VALUES (3, '测试部门', 'QA001', 1, '0,1', 1, 1, 2, NULL, 2, now(), 0);

-- ----------------------------
-- Table structure for sys_dict
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ',
                            `dict_code` varchar(50) COMMENT '类型编码',
                            `name` varchar(50) COMMENT '类型名称',
                            `status` tinyint(1) DEFAULT '0' COMMENT '状态(0:正常;1:禁用)',
                            `remark` varchar(255) COMMENT '备注',
                            `create_time` datetime COMMENT '创建时间',
                            `create_by` bigint COMMENT '创建人ID',
                            `update_time` datetime COMMENT '更新时间',
                            `update_by` bigint COMMENT '修改人ID',
                            `is_deleted` tinyint DEFAULT '0' COMMENT '是否删除(1-删除，0-未删除)',
                            PRIMARY KEY (`id`) USING BTREE,
                            KEY `idx_dict_code` (`dict_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典类型表';
-- ----------------------------
-- Records of sys_dict
-- ----------------------------
INSERT INTO `sys_dict` VALUES (1, 'gender', '性别', 1, NULL, now() , 1,now(), 1,0);
INSERT INTO `sys_dict` VALUES (2, 'notice_type', '通知类型', 1, NULL, now(), 1,now(), 1,0);
INSERT INTO `sys_dict` VALUES (3, 'notice_level', '通知级别', 1, NULL, now(), 1,now(), 1,0);


-- ----------------------------
-- Table structure for sys_dict_item
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_item`;
CREATE TABLE `sys_dict_item` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                 `dict_code` varchar(50) COMMENT '关联字典编码，与sys_dict表中的dict_code对应',
                                 `value` varchar(50) COMMENT '字典项值',
                                 `label` varchar(100) COMMENT '字典项标签',
                                 `tag_type` varchar(50) COMMENT '标签类型，用于前端样式展示（如success、warning等）',
                                 `status` tinyint DEFAULT '0' COMMENT '状态（1-正常，0-禁用）',
                                 `sort` int DEFAULT '0' COMMENT '排序',
                                 `remark` varchar(255) COMMENT '备注',
                                 `create_time` datetime COMMENT '创建时间',
                                 `create_by` bigint COMMENT '创建人ID',
                                 `update_time` datetime COMMENT '更新时间',
                                 `update_by` bigint COMMENT '修改人ID',
                                 PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典项表';

-- ----------------------------
-- Records of sys_dict_item
-- ----------------------------
INSERT INTO `sys_dict_item` VALUES (1, 'gender', '1', '男', 'primary', 1, 1, NULL, now(), 1,now(),1);
INSERT INTO `sys_dict_item` VALUES (2, 'gender', '2', '女', 'danger', 1, 2, NULL, now(), 1,now(),1);
INSERT INTO `sys_dict_item` VALUES (3, 'gender', '0', '保密', 'info', 1, 3, NULL, now(), 1,now(),1);
INSERT INTO `sys_dict_item` VALUES (4, 'notice_type', '1', '系统升级', 'success', 1, 1, '', now(), 1,now(),1);
INSERT INTO `sys_dict_item` VALUES (5, 'notice_type', '2', '系统维护', 'primary', 1, 2, '', now(), 1,now(),1);
INSERT INTO `sys_dict_item` VALUES (6, 'notice_type', '3', '安全警告', 'danger', 1, 3, '', now(), 1,now(),1);
INSERT INTO `sys_dict_item` VALUES (7, 'notice_type', '4', '假期通知', 'success', 1, 4, '', now(), 1,now(),1);
INSERT INTO `sys_dict_item` VALUES (8, 'notice_type', '5', '公司新闻', 'primary', 1, 5, '', now(), 1,now(),1);
INSERT INTO `sys_dict_item` VALUES (9, 'notice_type', '99', '其他', 'info', 1, 99, '', now(), 1,now(),1);
INSERT INTO `sys_dict_item` VALUES (10, 'notice_level', 'L', '低', 'info', 1, 1, '', now(), 1,now(),1);
INSERT INTO `sys_dict_item` VALUES (11, 'notice_level', 'M', '中', 'warning', 1, 2, '', now(), 1,now(),1);
INSERT INTO `sys_dict_item` VALUES (12, 'notice_level', 'H', '高', 'danger', 1, 3, '', now(), 1,now(),1);

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                             `parent_id` bigint NOT NULL COMMENT '父菜单ID',
                             `tree_path` varchar(255) COMMENT '父节点ID路径',
                             `name` varchar(64) NOT NULL COMMENT '菜单名称',
                             `type` char(1) NOT NULL COMMENT '菜单类型（C-目录 M-菜单 E-外链 B-按钮）',
                             `route_name` varchar(255) COMMENT '路由名称（Vue Router 中用于命名路由）',
                             `route_path` varchar(128) COMMENT '路由路径（Vue Router 中定义的 URL 路径）',
                             `component` varchar(128) COMMENT '组件路径（组件页面完整路径，相对于 src/views/，缺省后缀 .vue）',
                             `external_url` varchar(512) COMMENT '外链地址',
                             `perm` varchar(128) COMMENT '【按钮】权限标识',
                             `always_show` tinyint DEFAULT 0 COMMENT '【目录】只有一个子路由是否始终显示（1-是 0-否）',
                             `keep_alive` tinyint DEFAULT 0 COMMENT '【菜单】是否开启页面缓存（1-是 0-否）',
                             `visible` tinyint(1) DEFAULT 1 COMMENT '显示状态（1-显示 0-隐藏）',
                             `sort` int DEFAULT 0 COMMENT '排序',
                             `icon` varchar(64) COMMENT '菜单图标',
                             `redirect` varchar(128) COMMENT '跳转路径',
                             `create_time` datetime NULL COMMENT '创建时间',
                             `update_time` datetime NULL COMMENT '更新时间',
                             `params` json NULL COMMENT '路由参数',
                             PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '系统菜单表';

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
-- 顶级菜单（基础段 ID=sort 1-6）：系统管理(1)/代码生成(2)/通用组件(3)/多级菜单(4)/路由参数(5)/项目资源(6)
-- 扩展段 7-99 由 sql/extensions/ 脚本占用：动态表单(7,dynamic-form.sql)/工作流(8,workflow.sql)，新扩展模块从 9 领号顺延
-- ID 规则：子 ID = 父 ID × 100 + 两位序号（如 10101 即用户管理 101 的第 1 个按钮），tree_path 与 ID 同构可机器校验
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (1, 0, '0', '系统管理', 'C', '', '/system', 'Layout', NULL, NULL, NULL, 1, 1, 'system', '/system/user', now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (2, 0, '0', '代码生成', 'C', '', '/codegen', 'Layout', NULL, NULL, NULL, 1, 2, 'code', '/codegen/index', now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (6, 0, '0', '项目资源', 'C', '', '/resource', 'Layout', NULL, NULL, NULL, 1, 6, 'document', '/resource/practice-doc', now(), now(), NULL);

-- 系统管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (101, 1, '0,1', '用户管理', 'M', 'User', 'user', 'system/user/index', NULL, NULL, 1, 1, 1, 'el-icon-User', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10101, 101, '0,1,101', '用户查询', 'B', NULL, '', NULL, 'sys:user:list', NULL, NULL, 1, 1, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10102, 101, '0,1,101', '用户新增', 'B', NULL, '', NULL, 'sys:user:create', NULL, NULL, 1, 2, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10103, 101, '0,1,101', '用户编辑', 'B', NULL, '', NULL, 'sys:user:update', NULL, NULL, 1, 3, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10104, 101, '0,1,101', '用户删除', 'B', NULL, '', NULL, 'sys:user:delete', NULL, NULL, 1, 4, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10105, 101, '0,1,101', '重置密码', 'B', NULL, '', NULL, 'sys:user:reset-password', NULL, NULL, 1, 5, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10106, 101, '0,1,101', '用户导入', 'B', NULL, '', NULL, 'sys:user:import', NULL, NULL, 1, 6, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10107, 101, '0,1,101', '用户导出', 'B', NULL, '', NULL, 'sys:user:export', NULL, NULL, 1, 7, '', NULL, now(), now(), NULL);

INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (102, 1, '0,1', '角色管理', 'M', 'Role', 'role', 'system/role/index', NULL, NULL, 1, 1, 2, 'role', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10201, 102, '0,1,102', '角色查询', 'B', NULL, '', NULL, 'sys:role:list', NULL, NULL, 1, 1, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10202, 102, '0,1,102', '角色新增', 'B', NULL, '', NULL, 'sys:role:create', NULL, NULL, 1, 2, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10203, 102, '0,1,102', '角色编辑', 'B', NULL, '', NULL, 'sys:role:update', NULL, NULL, 1, 3, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10204, 102, '0,1,102', '角色删除', 'B', NULL, '', NULL, 'sys:role:delete', NULL, NULL, 1, 4, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10205, 102, '0,1,102', '角色分配权限', 'B', NULL, '', NULL, 'sys:role:assign', NULL, NULL, 1, 5, '', NULL, now(), now(), NULL);

INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (103, 1, '0,1', '菜单管理', 'M', 'SysMenu', 'menu', 'system/menu/index', NULL, NULL, 1, 1, 3, 'menu', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10301, 103, '0,1,103', '菜单查询', 'B', NULL, '', NULL, 'sys:menu:list', NULL, NULL, 1, 1, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10302, 103, '0,1,103', '菜单新增', 'B', NULL, '', NULL, 'sys:menu:create', NULL, NULL, 1, 2, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10303, 103, '0,1,103', '菜单编辑', 'B', NULL, '', NULL, 'sys:menu:update', NULL, NULL, 1, 3, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10304, 103, '0,1,103', '菜单删除', 'B', NULL, '', NULL, 'sys:menu:delete', NULL, NULL, 1, 4, '', NULL, now(), now(), NULL);

INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (104, 1, '0,1', '部门管理', 'M', 'Dept', 'dept', 'system/dept/index', NULL, NULL, 1, 1, 4, 'tree', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10401, 104, '0,1,104', '部门查询', 'B', NULL, '', NULL, 'sys:dept:list', NULL, NULL, 1, 1, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10402, 104, '0,1,104', '部门新增', 'B', NULL, '', NULL, 'sys:dept:create', NULL, NULL, 1, 2, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10403, 104, '0,1,104', '部门编辑', 'B', NULL, '', NULL, 'sys:dept:update', NULL, NULL, 1, 3, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10404, 104, '0,1,104', '部门删除', 'B', NULL, '', NULL, 'sys:dept:delete', NULL, NULL, 1, 4, '', NULL, now(), now(), NULL);

INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (105, 1, '0,1', '字典管理', 'M', 'Dict', 'dict', 'system/dict/index', NULL, NULL, 1, 1, 5, 'dict', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10501, 105, '0,1,105', '字典查询', 'B', NULL, '', NULL, 'sys:dict:list', NULL, NULL, 1, 1, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10502, 105, '0,1,105', '字典新增', 'B', NULL, '', NULL, 'sys:dict:create', NULL, NULL, 1, 2, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10503, 105, '0,1,105', '字典编辑', 'B', NULL, '', NULL, 'sys:dict:update', NULL, NULL, 1, 3, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10504, 105, '0,1,105', '字典删除', 'B', NULL, '', NULL, 'sys:dict:delete', NULL, NULL, 1, 4, '', NULL, now(), now(), NULL);

INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (106, 1, '0,1', '字典项', 'M', 'DictItem', 'dict-item', 'system/dict/dict-item', NULL, 0, 1, 0, 6, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10601, 106, '0,1,106', '字典项查询', 'B', NULL, '', NULL, 'sys:dict-item:list', NULL, NULL, 1, 1, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10602, 106, '0,1,106', '字典项新增', 'B', NULL, '', NULL, 'sys:dict-item:create', NULL, NULL, 1, 2, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10603, 106, '0,1,106', '字典项编辑', 'B', NULL, '', NULL, 'sys:dict-item:update', NULL, NULL, 1, 3, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10604, 106, '0,1,106', '字典项删除', 'B', NULL, '', NULL, 'sys:dict-item:delete', NULL, NULL, 1, 4, '', NULL, now(), now(), NULL);

INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (107, 1, '0,1', '系统日志', 'M', 'Log', 'log', 'system/log/index', NULL, 0, 1, 1, 7, 'document', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10701, 107, '0,1,107', '日志查询', 'B', NULL, '', NULL, 'sys:log:list', NULL, NULL, 1, 1, '', NULL, now(), now(), NULL);

INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (108, 1, '0,1', '系统配置', 'M', 'Config', 'config', 'system/config/index', NULL, 0, 1, 1, 8, 'setting', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10801, 108, '0,1,108', '系统配置查询', 'B', NULL, '', NULL, 'sys:config:list', 0, 1, 1, 1, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10802, 108, '0,1,108', '系统配置新增', 'B', NULL, '', NULL, 'sys:config:create', 0, 1, 1, 2, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10803, 108, '0,1,108', '系统配置修改', 'B', NULL, '', NULL, 'sys:config:update', 0, 1, 1, 3, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10804, 108, '0,1,108', '系统配置删除', 'B', NULL, '', NULL, 'sys:config:delete', 0, 1, 1, 4, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10805, 108, '0,1,108', '系统配置刷新', 'B', NULL, '', NULL, 'sys:config:refresh', 0, 1, 1, 5, '', NULL, now(), now(), NULL);

INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (109, 1, '0,1', '通知公告', 'M', 'Notice', 'notice', 'system/notice/index', NULL, NULL, NULL, 1, 9, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10901, 109, '0,1,109', '通知查询', 'B', NULL, '', NULL, 'sys:notice:list', NULL, NULL, 1, 1, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10902, 109, '0,1,109', '通知新增', 'B', NULL, '', NULL, 'sys:notice:create', NULL, NULL, 1, 2, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10903, 109, '0,1,109', '通知编辑', 'B', NULL, '', NULL, 'sys:notice:update', NULL, NULL, 1, 3, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10904, 109, '0,1,109', '通知删除', 'B', NULL, '', NULL, 'sys:notice:delete', NULL, NULL, 1, 4, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10905, 109, '0,1,109', '通知发布', 'B', NULL, '', NULL, 'sys:notice:publish', 0, 1, 1, 5, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (10906, 109, '0,1,109', '通知撤回', 'B', NULL, '', NULL, 'sys:notice:revoke', 0, 1, 1, 6, '', NULL, now(), now(), NULL);

-- 代码生成
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (201, 2, '0,2', '代码生成', 'M', 'Codegen', 'index', 'codegen/index', NULL, NULL, 1, 1, 1, 'code', NULL, now(), now(), NULL);

-- 项目资源（原"项目文档"；范围扩展为文档 + 接口 + 源码仓库的资源导航）
-- 实战文档双形态置顶（内嵌 iframe + 外链新标签），直观演示系统对两种外链打开方式的支持
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `external_url`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (601, 6, '0,6', '实战文档(内嵌)', 'E', 'PracticeDoc', 'practice-doc', 'iframe', 'https://juejin.cn/post/7228990409909108793', NULL, NULL, 1, 1, 1, 'el-icon-Document', '', now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `external_url`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (602, 6, '0,6', '实战文档(外链)', 'E', NULL, NULL, NULL, 'https://juejin.cn/post/7228990409909108793', NULL, NULL, NULL, 1, 2, 'el-icon-Link', '', now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `external_url`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (603, 6, '0,6', '接口文档', 'E', 'Apifox', 'apifox', 'iframe', 'https://www.apifox.cn/apidoc/shared-195e783f-4d85-4235-a038-eec696de4ea5', NULL, NULL, 1, 1, 3, 'api', '', now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `external_url`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (604, 6, '0,6', '后端文档', 'E', NULL, NULL, NULL, 'https://youlai.blog.csdn.net/article/details/145178880', NULL, NULL, NULL, 1, 4, 'document', '', now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `external_url`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (605, 6, '0,6', '移动端文档', 'E', NULL, NULL, NULL, 'https://youlai.blog.csdn.net/article/details/143222890', NULL, NULL, NULL, 1, 5, 'document', '', now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `external_url`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (606, 6, '0,6', '前端仓库', 'E', NULL, NULL, NULL, 'https://gitee.com/youlaiorg/vue3-element-admin', NULL, NULL, NULL, 1, 6, 'gitee', '', now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `external_url`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (607, 6, '0,6', '后端仓库', 'E', NULL, NULL, NULL, 'https://gitee.com/youlaiorg/youlai-boot', NULL, NULL, NULL, 1, 7, 'gitee', '', now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `external_url`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (608, 6, '0,6', '移动端仓库', 'E', NULL, NULL, NULL, 'https://gitee.com/youlaiorg/vue-uniapp-template', NULL, NULL, NULL, 1, 8, 'gitee', '', now(), now(), NULL);

-- ============================ 通用组件（顶级 ID=3） ============================
-- 前端模板招牌能力聚合，独立顶级便于访客沉淀组件库；按用途三分组：基础/表单/表格
-- 物理目录 views/demo/component（基础与表单）与 views/demo/table（表格）一一对应
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (3, 0, '0', '通用组件', 'C', '', '/component', 'Layout', NULL, NULL, NULL, 1, 3, 'el-icon-SetUp', '/component/basic/icon-demo', now(), now(), NULL);

-- 基础组件（不依附表单/表格场景的通用 UI 能力）
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (301, 3, '0,3', '基础组件', 'C', '', 'basic', 'Layout', NULL, 1, NULL, 1, 1, 'el-icon-Brush', '', now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (30101, 301, '0,3,301', 'Icons', 'M', 'IconDemo', 'icon-demo', 'demo/component/icons', NULL, NULL, 1, 1, 1, 'el-icon-Notification', '', now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (30102, 301, '0,3,301', '拖拽组件', 'M', 'Drag', 'drag', 'demo/component/drag', NULL, NULL, NULL, 1, 2, '', '', now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (30103, 301, '0,3,301', '滚动文本', 'M', 'TextScroll', 'text-scroll', 'demo/component/text-scroll', NULL, NULL, NULL, 1, 3, '', '', now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (30104, 301, '0,3,301', '字典实时同步', 'M', 'DictSync', 'dict-sync', 'demo/component/dict-sync', NULL, NULL, NULL, 1, 4, '', '', now(), now(), NULL);

-- 表单组件（数据录入场景）
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (302, 3, '0,3', '表单组件', 'C', '', 'form', 'Layout', NULL, 1, NULL, 1, 2, 'el-icon-EditPen', '', now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (30201, 302, '0,3,302', '富文本编辑器', 'M', 'WangEditor', 'wang-editor', 'demo/component/wang-editor', NULL, NULL, 1, 1, 1, '', '', now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (30202, 302, '0,3,302', '图片上传', 'M', 'Upload', 'upload', 'demo/component/upload', NULL, NULL, 1, 1, 2, '', '', now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (30203, 302, '0,3,302', '图标选择器', 'M', 'DemoIconSelect', 'icon-select', 'demo/component/icon-select', NULL, NULL, 1, 1, 3, '', '', now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (30204, 302, '0,3,302', '字典组件', 'M', 'DictDemo', 'dict-demo', 'demo/component/dictionary', NULL, NULL, 1, 1, 4, '', '', now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (30205, 302, '0,3,302', '列表选择器', 'M', 'TableSelect', 'table-select', 'demo/component/table-select/index', NULL, NULL, 1, 1, 5, '', '', now(), now(), NULL);

-- 表格组件（数据展示场景，物理目录 views/demo/table 与之对应）
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (303, 3, '0,3', '表格组件', 'C', '', 'table', 'Layout', NULL, 1, NULL, 1, 3, 'el-icon-Grid', '', now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (30301, 303, '0,3,303', '增删改查', 'M', 'Curd', 'curd', 'demo/table/curd/index', NULL, NULL, 1, 1, 1, '', '', now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (30302, 303, '0,3,303', 'CURD单文件', 'M', 'CurdSingle', 'curd-single', 'demo/table/curd-single', NULL, NULL, 1, 1, 2, 'el-icon-Reading', '', now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (30303, 303, '0,3,303', 'VxeTable', 'M', 'VxeTable', 'vxe-table', 'demo/table/vxe-table/index', NULL, NULL, 1, 1, 3, 'el-icon-MagicStick', '', now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (30304, 303, '0,3,303', '自适应表格操作列', 'M', 'AutoOperationColumn', 'operation-column', 'demo/table/auto-operation-column', NULL, NULL, 1, 1, 4, '', '', now(), now(), NULL);

-- ============================ 多级菜单（顶级 ID=4） ============================
-- 独立顶级，直观演示菜单驱动动态路由的嵌套能力，避免折叠导致顶级菜单不可见
-- 物理目录 views/demo/route/multi-level 与之对应
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (4, 0, '0', '多级菜单', 'C', '', '/multi-level', 'Layout', NULL, 1, NULL, 1, 4, 'el-icon-Guide', '/multi-level/level-one/level-two/level-three-a', now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (401, 4, '0,4', '一级菜单', 'C', '', 'level-one', 'Layout', NULL, 1, NULL, 1, 1, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (40101, 401, '0,4,401', '二级菜单', 'C', '', 'level-two', 'Layout', NULL, 0, NULL, 1, 1, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (4010101, 40101, '0,4,401,40101', '三级菜单 A', 'M', 'MultiLevelLevelThreeA', 'level-three-a', 'demo/route/multi-level/level-one/level-two/level-three-a/index', NULL, 0, 1, 1, 1, '', '', now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (4010102, 40101, '0,4,401,40101', '三级菜单 B', 'M', 'MultiLevelLevelThreeB', 'level-three-b', 'demo/route/multi-level/level-one/level-two/level-three-b/index', NULL, 0, 1, 1, 2, '', '', now(), now(), NULL);

-- ============================ 路由参数（顶级 ID=5） ============================
-- 同一页面按 params 区分菜单入口的传参演示；详情页缓存经增删改查详情 /detail/:id 进入，无独立菜单
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (5, 0, '0', '路由参数', 'C', '', '/route-param', 'Layout', NULL, 1, NULL, 1, 5, 'el-icon-Share', '/route-param/route-param-type1', now(), now(), NULL);
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (501, 5, '0,5', '路由参数(type=1)', 'M', 'RouteParamType1', 'route-param-type1', 'demo/route/route-param', NULL, 0, 1, 1, 1, 'el-icon-Star', NULL, now(), now(), '{\"type\": \"1\"}');
INSERT INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (502, 5, '0,5', '路由参数(type=2)', 'M', 'RouteParamType2', 'route-param-type2', 'demo/route/route-param', NULL, 0, 1, 1, 2, 'el-icon-StarFilled', NULL, now(), now(), '{\"type\": \"2\"}');

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
                             `id` bigint NOT NULL AUTO_INCREMENT,
                             `name` varchar(64) NOT NULL COMMENT '角色名称',
                             `code` varchar(32) NOT NULL COMMENT '角色编码',
                             `sort` int NULL COMMENT '显示顺序',
                             `status` tinyint(1) DEFAULT 1 COMMENT '角色状态(1-正常 0-停用)',
                             `data_scope` tinyint NULL COMMENT '数据权限(1-所有数据 2-部门及子部门数据 3-本部门数据 4-本人数据 5-自定义部门数据)',
                             `create_by` bigint NULL COMMENT '创建人 ID',
                             `create_time` datetime NULL COMMENT '创建时间',
                             `update_by` bigint NULL COMMENT '更新人ID',
                             `update_time` datetime NULL COMMENT '更新时间',
                             `is_deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除标识(0-未删除 1-已删除)',
                             PRIMARY KEY (`id`) USING BTREE,
                             UNIQUE INDEX `uk_name`(`name` ASC) USING BTREE COMMENT '角色名称唯一索引',
                             UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE COMMENT '角色编码唯一索引'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '系统角色表';

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '超级管理员', 'ROOT', 1, 1, 1, NULL, now(), NULL, now(), 0);
INSERT INTO `sys_role` VALUES (2, '系统管理员', 'ADMIN', 2, 1, 1, NULL, now(), NULL, NULL, 0);
INSERT INTO `sys_role` VALUES (3, '访问游客', 'GUEST', 3, 1, 3, NULL, now(), NULL, now(), 0);
INSERT INTO `sys_role` VALUES (4, '部门主管', 'DEPT_MANAGER', 4, 1, 2, NULL, now(), NULL, now(), 0);
INSERT INTO `sys_role` VALUES (5, '部门成员', 'DEPT_MEMBER', 5, 1, 3, NULL, now(), NULL, now(), 0);
INSERT INTO `sys_role` VALUES (6, '普通员工', 'EMPLOYEE', 6, 1, 4, NULL, now(), NULL, now(), 0);
INSERT INTO `sys_role` VALUES (7, '自定义权限用户', 'CUSTOM_USER', 7, 1, 5, NULL, now(), NULL, now(), 0);

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
                                  `role_id` bigint NOT NULL COMMENT '角色ID',
                                  `menu_id` bigint NOT NULL COMMENT '菜单ID',
                                  UNIQUE INDEX `uk_roleid_menuid`(`role_id` ASC, `menu_id` ASC) USING BTREE COMMENT '角色菜单唯一索引'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '角色菜单关联表';

-- ----------------------------
-- Table structure for sys_role_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept`  (
                                  `role_id` bigint NOT NULL COMMENT '角色ID',
                                  `dept_id` bigint NOT NULL COMMENT '部门ID',
                                  UNIQUE INDEX `uk_roleid_deptid`(`role_id` ASC, `dept_id` ASC) USING BTREE COMMENT '角色部门唯一索引'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '角色部门关联表';

-- ----------------------------
-- Records of sys_role_dept
-- ----------------------------
INSERT IGNORE INTO `sys_role_dept` VALUES (7, 1);
INSERT IGNORE INTO `sys_role_dept` VALUES (7, 2);

-- ============================================
-- 系统管理员角色菜单权限（role_id=2，基础段顶级 1-6 全量）
INSERT INTO `sys_role_menu` VALUES (2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 6);
-- 系统管理
INSERT INTO `sys_role_menu` VALUES (2, 101), (2, 10101), (2, 10102), (2, 10103), (2, 10104), (2, 10105), (2, 10106), (2, 10107);
INSERT INTO `sys_role_menu` VALUES (2, 102), (2, 10201), (2, 10202), (2, 10203), (2, 10204), (2, 10205);
INSERT INTO `sys_role_menu` VALUES (2, 103), (2, 10301), (2, 10302), (2, 10303), (2, 10304);
INSERT INTO `sys_role_menu` VALUES (2, 104), (2, 10401), (2, 10402), (2, 10403), (2, 10404);
INSERT INTO `sys_role_menu` VALUES (2, 105), (2, 10501), (2, 10502), (2, 10503), (2, 10504);
INSERT INTO `sys_role_menu` VALUES (2, 106), (2, 10601), (2, 10602), (2, 10603), (2, 10604);
INSERT INTO `sys_role_menu` VALUES (2, 107), (2, 10701);
INSERT INTO `sys_role_menu` VALUES (2, 108), (2, 10801), (2, 10802), (2, 10803), (2, 10804), (2, 10805);
INSERT INTO `sys_role_menu` VALUES (2, 109), (2, 10901), (2, 10902), (2, 10903), (2, 10904), (2, 10905), (2, 10906);

INSERT IGNORE INTO `sys_role_menu` VALUES (4, 1);
INSERT IGNORE INTO `sys_role_menu` VALUES (4, 101), (4, 10101), (4, 10102), (4, 10103), (4, 10104), (4, 10105), (4, 10106), (4, 10107);
INSERT IGNORE INTO `sys_role_menu` VALUES (4, 102), (4, 10201), (4, 10202), (4, 10203), (4, 10204), (4, 10205);

INSERT IGNORE INTO `sys_role_menu` VALUES (5, 1);
INSERT IGNORE INTO `sys_role_menu` VALUES (5, 101), (5, 10101), (5, 10102), (5, 10103), (5, 10104), (5, 10105), (5, 10106), (5, 10107);
INSERT IGNORE INTO `sys_role_menu` VALUES (5, 102), (5, 10201), (5, 10202), (5, 10203), (5, 10204), (5, 10205);

INSERT IGNORE INTO `sys_role_menu` VALUES (6, 1);
INSERT IGNORE INTO `sys_role_menu` VALUES (6, 101), (6, 10101), (6, 10102), (6, 10103), (6, 10104), (6, 10105), (6, 10106), (6, 10107);
INSERT IGNORE INTO `sys_role_menu` VALUES (6, 102), (6, 10201), (6, 10202), (6, 10203), (6, 10204), (6, 10205);

INSERT IGNORE INTO `sys_role_menu` VALUES (7, 1);
INSERT IGNORE INTO `sys_role_menu` VALUES (7, 101), (7, 10101), (7, 10102), (7, 10103), (7, 10104), (7, 10105), (7, 10106), (7, 10107);
INSERT IGNORE INTO `sys_role_menu` VALUES (7, 102), (7, 10201), (7, 10202), (7, 10203), (7, 10204), (7, 10205);
-- 代码生成
INSERT INTO `sys_role_menu` VALUES (2, 201);
-- 项目资源
INSERT INTO `sys_role_menu` VALUES (2, 601), (2, 602), (2, 603), (2, 604), (2, 605), (2, 606), (2, 607), (2, 608);
-- 通用组件（301 基础 / 302 表单 / 303 表格）
INSERT INTO `sys_role_menu` VALUES (2, 301), (2, 30101), (2, 30102), (2, 30103), (2, 30104);
INSERT INTO `sys_role_menu` VALUES (2, 302), (2, 30201), (2, 30202), (2, 30203), (2, 30204), (2, 30205);
INSERT INTO `sys_role_menu` VALUES (2, 303), (2, 30301), (2, 30302), (2, 30303), (2, 30304);
-- 多级菜单 / 路由参数
INSERT INTO `sys_role_menu` VALUES (2, 401), (2, 40101), (2, 4010101), (2, 4010102);
INSERT INTO `sys_role_menu` VALUES (2, 501), (2, 502);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
                             `id` bigint NOT NULL AUTO_INCREMENT,
                             `username` varchar(64) COMMENT '用户名',
                             `nickname` varchar(64) COMMENT '昵称',
                             `gender` tinyint(1) DEFAULT 1 COMMENT '性别((1-男 2-女 0-保密)',
                             `password` varchar(100) COMMENT '密码',
                             `dept_id` int COMMENT '部门ID',
                             `avatar` varchar(255) COMMENT '用户头像',
                             `mobile` varchar(20) COMMENT '联系方式',
                             `status` tinyint(1) DEFAULT 1 COMMENT '状态(1-正常 0-禁用)',
                             `email` varchar(128) COMMENT '用户邮箱',
                             `create_time` datetime COMMENT '创建时间',
                             `create_by` bigint COMMENT '创建人ID',
                             `update_time` datetime COMMENT '更新时间',
                             `update_by` bigint COMMENT '修改人ID',
                             `is_deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除标识(0-未删除 1-已删除)',
                            PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '系统用户表';

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'youlai', '有来技术', 0, '$2a$10$xVWsNOhHrCxh5UbpCE7/HuJ.PAOKcYAqRxD2CO2nVnJS.IAXkr5aq', NULL, 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', '18812345677', 1, 'youlaitech@163.com', now(), NULL, now(), NULL, 0);
INSERT INTO `sys_user` VALUES (2, 'admin', '系统管理员', 1, '$2a$10$xVWsNOhHrCxh5UbpCE7/HuJ.PAOKcYAqRxD2CO2nVnJS.IAXkr5aq', 1, 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', '18888888888', 1, 'youlaitech@163.com', now(), NULL, now(), NULL, 0);
INSERT INTO `sys_user` VALUES (3, 'test', '测试小用户', 1, '$2a$10$xVWsNOhHrCxh5UbpCE7/HuJ.PAOKcYAqRxD2CO2nVnJS.IAXkr5aq', 3, 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', '18812345679', 1, 'youlaitech@163.com', now(), NULL, now(), NULL, 0);
INSERT INTO `sys_user` VALUES (4, 'dept_manager', '部门主管', 1, '$2a$10$xVWsNOhHrCxh5UbpCE7/HuJ.PAOKcYAqRxD2CO2nVnJS.IAXkr5aq', 1, 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', '18812345680', 1, 'manager@youlaitech.com', now(), NULL, now(), NULL, 0);
INSERT INTO `sys_user` VALUES (5, 'dept_member', '部门成员', 1, '$2a$10$xVWsNOhHrCxh5UbpCE7/HuJ.PAOKcYAqRxD2CO2nVnJS.IAXkr5aq', 1, 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', '18812345681', 1, 'member@youlaitech.com', now(), NULL, now(), NULL, 0);
INSERT INTO `sys_user` VALUES (6, 'employee', '普通员工', 1, '$2a$10$xVWsNOhHrCxh5UbpCE7/HuJ.PAOKcYAqRxD2CO2nVnJS.IAXkr5aq', 2, 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', '18812345682', 1, 'employee@youlaitech.com', now(), NULL, now(), NULL, 0);
INSERT INTO `sys_user` VALUES (7, 'custom_user', '自定义权限用户', 1, '$2a$10$xVWsNOhHrCxh5UbpCE7/HuJ.PAOKcYAqRxD2CO2nVnJS.IAXkr5aq', 3, 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', '18812345683', 1, 'custom@youlaitech.com', now(), NULL, now(), NULL, 0);

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
                                  `user_id` bigint NOT NULL COMMENT '用户ID',
                                  `role_id` bigint NOT NULL COMMENT '角色ID',
                                  PRIMARY KEY (`user_id`, `role_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '用户角色关联表';

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT IGNORE INTO `sys_user_role` VALUES (1, 1);
INSERT IGNORE INTO `sys_user_role` VALUES (2, 2);
INSERT IGNORE INTO `sys_user_role` VALUES (3, 3);
INSERT IGNORE INTO `sys_user_role` VALUES (4, 4);
INSERT IGNORE INTO `sys_user_role` VALUES (5, 5);
INSERT IGNORE INTO `sys_user_role` VALUES (6, 6);
INSERT IGNORE INTO `sys_user_role` VALUES (7, 7);


-- ----------------------------
-- Table structure for sys_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `module` TINYINT NOT NULL COMMENT '模块，数字枚举，参考 LogModule 枚举',
    `action_type` TINYINT NOT NULL COMMENT '操作类型，数字枚举，参考 ActionType 枚举',
    `title` VARCHAR(100) NOT NULL COMMENT '前端显示标题',
    `content` VARCHAR(500) COMMENT '自定义日志内容',
    `operator_id` BIGINT COMMENT '操作人ID',
    `operator_name` VARCHAR(50) COMMENT '操作人名称',
    `request_uri` VARCHAR(255) COMMENT '请求路径',
    `request_method` VARCHAR(10) COMMENT '请求方法',
    `ip` VARCHAR(45) COMMENT 'IP地址',
    `province` VARCHAR(100) COMMENT '省份',
    `city` VARCHAR(100) COMMENT '城市',
    `device` VARCHAR(100) COMMENT '设备',
    `os` VARCHAR(100) COMMENT '操作系统',
    `browser` VARCHAR(100) COMMENT '浏览器',
    `status` TINYINT DEFAULT 1 COMMENT '0失败 1成功',
    `error_msg` VARCHAR(1000) COMMENT '错误信息',
    `execution_time` INT COMMENT '执行时间(ms)',
    `create_time` DATETIME COMMENT '操作时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_module_action_time` (`module`, `action_type`, `create_time`),
    KEY `idx_operator_time` (`operator_id`, `create_time`),
    KEY `idx_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表';

-- ----------------------------
-- Table structure for gen_table
-- ----------------------------
DROP TABLE IF EXISTS `gen_table`;
CREATE TABLE `gen_table` (
                              `id` bigint NOT NULL AUTO_INCREMENT,
                              `table_name` varchar(100) NOT NULL COMMENT '表名',
                              `module_name` varchar(100) COMMENT '模块名',
                              `package_name` varchar(255) NOT NULL COMMENT '包名',
                              `business_name` varchar(100) NOT NULL COMMENT '业务名',
                              `entity_name` varchar(100) NOT NULL COMMENT '实体类名',
                              `author` varchar(50) NOT NULL COMMENT '作者',
                              `parent_menu_id` bigint COMMENT '上级菜单ID，对应sys_menu的id ',
                              `remove_table_prefix` varchar(20) COMMENT '要移除的表前缀，如: sys_',
                              `page_type` varchar(20) COMMENT '页面类型(classic|curd)',
                              `create_time` datetime COMMENT '创建时间',
                              `update_time` datetime COMMENT '更新时间',
                              `is_deleted` tinyint(4) DEFAULT 0 COMMENT '是否删除',
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_tablename` (`table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成配置表';

-- ----------------------------
-- Table structure for gen_table_column
-- ----------------------------
DROP TABLE IF EXISTS `gen_table_column`;
CREATE TABLE `gen_table_column` (
                                    `id` bigint NOT NULL AUTO_INCREMENT,
                                    `table_id` bigint NOT NULL COMMENT '关联的表配置ID',
                                    `column_name` varchar(100)  ,
                                    `column_type` varchar(50)  ,
                                    `column_length` int ,
                                    `field_name` varchar(100) NOT NULL COMMENT '字段名称',
                                    `field_type` varchar(100) COMMENT '字段类型',
                                    `field_sort` int COMMENT '字段排序',
                                    `field_comment` varchar(255) COMMENT '字段描述',
                                    `max_length` int ,
                                    `is_required` tinyint(1) COMMENT '是否必填',
                                    `is_show_in_list` tinyint(1) DEFAULT '0' COMMENT '是否在列表显示',
                                    `is_show_in_form` tinyint(1) DEFAULT '0' COMMENT '是否在表单显示',
                                    `is_show_in_query` tinyint(1) DEFAULT '0' COMMENT '是否在查询条件显示',
                                    `query_type` tinyint COMMENT '查询方式',
                                    `form_type` tinyint COMMENT '表单类型',
                                    `dict_type` varchar(50) COMMENT '字典类型',
                                    `create_time` datetime COMMENT '创建时间',
                                    `update_time` datetime COMMENT '更新时间',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_table_id` (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成字段配置表';

-- ----------------------------
-- 系统配置表
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
                              `id` bigint NOT NULL AUTO_INCREMENT,
                              `config_name` varchar(50) NOT NULL COMMENT '配置名称',
                              `config_key` varchar(50) NOT NULL COMMENT '配置key',
                              `config_value` varchar(100) NOT NULL COMMENT '配置值',
                              `remark` varchar(255) COMMENT '备注',
                              `create_time` datetime COMMENT '创建时间',
                              `create_by` bigint COMMENT '创建人ID',
                              `update_time` datetime COMMENT '更新时间',
                              `update_by` bigint COMMENT '更新人ID',
                              `is_deleted` tinyint(4) DEFAULT '0' NOT NULL COMMENT '逻辑删除标识(0-未删除 1-已删除)',
                              PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='系统配置表';

INSERT INTO `sys_config` VALUES (1, '系统限流QPS', 'IP_QPS_THRESHOLD_LIMIT', '10', '单个IP请求的最大每秒查询数（QPS）阈值Key', now(), 1, NULL, NULL, 0);

-- ----------------------------
-- 通知公告表
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
                              `id` bigint NOT NULL AUTO_INCREMENT,
                              `title` varchar(50) COMMENT '通知标题',
                              `content` text COMMENT '通知内容',
                              `type` tinyint NOT NULL COMMENT '通知类型（关联字典编码：notice_type）',
                              `level` varchar(5) NOT NULL COMMENT '通知等级（字典code：notice_level）',
                              `target_type` tinyint NOT NULL COMMENT '目标类型（1: 全体, 2: 指定）',
                              `target_user_ids` varchar(255) COMMENT '目标人ID集合（多个使用英文逗号,分割）',
                              `publisher_id` bigint COMMENT '发布人ID',
                              `publish_status` tinyint DEFAULT '0' COMMENT '发布状态（0: 未发布, 1: 已发布, -1: 已撤回）',
                              `publish_time` datetime COMMENT '发布时间',
                              `revoke_time` datetime COMMENT '撤回时间',
                              `create_by` bigint NOT NULL COMMENT '创建人ID',
                              `create_time` datetime NOT NULL COMMENT '创建时间',
                              `update_by` bigint COMMENT '更新人ID',
                              `update_time` datetime COMMENT '更新时间',
                              `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除（0: 未删除, 1: 已删除）',
                              PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知公告表';

INSERT INTO `sys_notice` VALUES (1, 'v3.0.0 版本发布 - 多租户功能上线', '<p>🎉 新版本发布，主要更新内容：</p><p>1. 新增多租户功能，支持租户隔离和数据管理</p><p>2. 优化系统性能，提升响应速度</p><p>3. 完善权限管理，增强安全性</p><p>4. 修复已知问题，提升系统稳定性</p>', 1, 'H', 1, NULL, 1, 1, '2024-12-15 10:00:00', NULL, 1, '2024-12-15 10:00:00', 1, '2024-12-15 10:00:00', 0);
INSERT INTO `sys_notice` VALUES (2, '系统维护通知 - 2024年12月20日', '<p>⏰ 系统维护通知</p><p>系统将于 <strong>2024年12月20日（本周五）凌晨 2:00-4:00</strong> 进行例行维护升级。</p><p>维护期间系统将暂停服务，请提前做好数据备份工作。</p><p>给您带来的不便，敬请谅解！</p>', 2, 'H', 1, NULL, 1, 1, '2024-12-18 14:30:00', NULL, 1, '2024-12-18 14:30:00', 1, '2024-12-18 14:30:00', 0);
INSERT INTO `sys_notice` VALUES (3, '安全提醒 - 防范钓鱼邮件', '<p>⚠️ 安全提醒</p><p>近期发现有不法分子通过钓鱼邮件进行网络攻击，请大家提高警惕：</p><p>1. 不要点击来源不明的邮件链接</p><p>2. 不要下载可疑附件</p><p>3. 遇到可疑邮件请及时联系IT部门</p><p>4. 定期修改密码，使用强密码策略</p>', 3, 'H', 1, NULL, 1, 1, '2024-12-10 09:00:00', NULL, 1, '2024-12-10 09:00:00', 1, '2024-12-10 09:00:00', 0);
INSERT INTO `sys_notice` VALUES (4, '元旦假期安排通知', '<p>📅 元旦假期安排</p><p>根据国家法定节假日安排，公司元旦假期时间为：</p><p><strong>2024年12月30日（周一）至 2025年1月1日（周三）</strong>，共3天。</p><p>2024年12月29日（周日）正常上班。</p><p>祝大家元旦快乐，假期愉快！</p>', 4, 'M', 1, NULL, 1, 1, '2024-12-25 16:00:00', NULL, 1, '2024-12-25 16:00:00', 1, '2024-12-25 16:00:00', 0);
INSERT INTO `sys_notice` VALUES (5, '新产品发布会邀请', '<p>🎊 新产品发布会邀请</p><p>公司将于 <strong>2025年1月15日下午14:00</strong> 在总部会议室举办新产品发布会。</p><p>届时将展示最新研发的产品和技术成果，欢迎全体员工参加。</p><p>请各部门提前安排好工作，准时参加。</p>', 5, 'M', 1, NULL, 1, 1, '2024-12-28 11:00:00', NULL, 1, '2024-12-28 11:00:00', 1, '2024-12-28 11:00:00', 0);
INSERT INTO `sys_notice` VALUES (6, 'v2.16.1 版本更新', '<p>✨ 版本更新</p><p>v2.16.1 版本已发布，主要修复内容：</p><p>1. 修复 WebSocket 重复连接导致的后台线程阻塞问题</p><p>2. 优化通知公告功能，提升用户体验</p><p>3. 修复部分已知bug</p><p>建议尽快更新到最新版本。</p>', 1, 'M', 1, NULL, 1, 1, '2024-12-05 15:30:00', NULL, 1, '2024-12-05 15:30:00', 1, '2024-12-05 15:30:00', 0);
INSERT INTO `sys_notice` VALUES (7, '年终总结会议通知', '<p>📋 年终总结会议通知</p><p>各部门年终总结会议将于 <strong>2024年12月30日上午9:00</strong> 召开。</p><p>请各部门负责人提前准备好年度工作总结和下年度工作计划。</p><p>会议地点：总部大会议室</p>', 5, 'M', 2, '1,2', 1, 1, '2024-12-22 10:00:00', NULL, 1, '2024-12-22 10:00:00', 1, '2024-12-22 10:00:00', 0);
INSERT INTO `sys_notice` VALUES (8, '系统功能优化完成', '<p>✅ 系统功能优化</p><p>已完成以下功能优化：</p><p>1. 优化用户管理界面，提升操作体验</p><p>2. 增强数据导出功能，支持更多格式</p><p>3. 优化搜索功能，提升查询效率</p><p>4. 修复部分界面显示问题</p>', 1, 'L', 1, NULL, 1, 1, '2024-12-12 14:20:00', NULL, 1, '2024-12-12 14:20:00', 1, '2024-12-12 14:20:00', 0);
INSERT INTO `sys_notice` VALUES (9, '员工培训计划', '<p>📚 员工培训计划</p><p>为提升员工专业技能，公司将于 <strong>2025年1月8日-10日</strong> 组织技术培训。</p><p>培训内容：</p><p>1. 新技术框架应用</p><p>2. 代码规范与最佳实践</p><p>3. 系统架构设计</p><p>请各部门合理安排工作，确保培训顺利进行。</p>', 5, 'M', 1, NULL, 1, 1, '2024-12-20 09:30:00', NULL, 1, '2024-12-20 09:30:00', 1, '2024-12-20 09:30:00', 0);
INSERT INTO `sys_notice` VALUES (10, '数据备份提醒', '<p>💾 数据备份提醒</p><p>请各部门注意定期备份重要数据，建议每周至少备份一次。</p><p>备份方式：</p><p>1. 使用系统自带备份功能</p><p>2. 手动导出重要数据</p><p>3. 联系IT部门协助备份</p><p>数据安全，人人有责！</p>', 3, 'L', 1, NULL, 1, 1, '2024-12-08 08:00:00', NULL, 1, '2024-12-08 08:00:00', 1, '2024-12-08 08:00:00', 0);

-- ----------------------------
-- 用户通知公告表
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_notice`;
CREATE TABLE `sys_user_notice` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
                                   `notice_id` bigint NOT NULL COMMENT '公共通知id',
                                   `user_id` bigint NOT NULL COMMENT '用户id',
                                   `is_read` tinyint DEFAULT '0' COMMENT '读取状态（0: 未读, 1: 已读）',
                                   `read_time` datetime COMMENT '阅读时间',
                                   `create_time` datetime NOT NULL COMMENT '创建时间',
                                   `update_time` datetime COMMENT '更新时间',
                                   `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除(0: 未删除, 1: 已删除)',
                                   PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知公告关联表';

INSERT INTO `sys_user_notice` VALUES (1, 1, 2, 1, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (2, 2, 2, 0, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (3, 3, 2, 0, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (4, 4, 2, 1, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (5, 5, 2, 0, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (6, 6, 2, 1, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (7, 7, 2, 0, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (8, 8, 2, 1, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (9, 9, 2, 0, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (10, 10, 2, 1, NULL, now(), now(), 0);

-- root 用户 (user_id=1) 绑定全部通知，5 条未读展示红点效果
INSERT INTO `sys_user_notice` VALUES (11, 1, 1, 0, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (12, 2, 1, 1, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (13, 3, 1, 0, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (14, 4, 1, 1, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (15, 5, 1, 0, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (16, 6, 1, 1, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (17, 7, 1, 0, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (18, 8, 1, 1, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (19, 9, 1, 0, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (20, 10, 1, 1, NULL, now(), now(), 0);

-- test 用户 (user_id=3) 绑定除指定通知(ID=7)外的 9 条通知，4 条未读
INSERT INTO `sys_user_notice` VALUES (21, 1, 3, 0, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (22, 2, 3, 1, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (23, 3, 3, 0, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (24, 4, 3, 1, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (25, 5, 3, 1, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (26, 6, 3, 0, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (27, 8, 3, 1, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (28, 9, 3, 1, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (29, 10, 3, 0, NULL, now(), now(), 0);

-- ----------------------------
-- Table structure for sys_user_social
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_social`;
CREATE TABLE `sys_user_social` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `platform` varchar(20) NOT NULL COMMENT '平台类型(WECHAT_MINI/WECHAT_MP/ALIPAY/QQ/APPLE)',
  `openid` varchar(64) NOT NULL COMMENT '平台openid',
  `unionid` varchar(64) DEFAULT NULL COMMENT '微信unionid',
  `nickname` varchar(64) DEFAULT NULL COMMENT '第三方昵称',
  `avatar` varchar(255) DEFAULT NULL COMMENT '第三方头像URL',
  `session_key` varchar(128) DEFAULT NULL COMMENT '微信session_key',
  `verified` tinyint(1) DEFAULT 1 COMMENT '是否已验证(1-已验证 0-未验证)',
  `create_time` datetime DEFAULT NULL COMMENT '绑定时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_openid` (`platform`, `openid`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_unionid` (`unionid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户第三方账号绑定表';

