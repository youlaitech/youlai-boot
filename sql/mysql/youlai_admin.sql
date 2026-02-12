
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
                             `type` char(1) NOT NULL COMMENT '菜单类型（C-目录 M-菜单 B-按钮）',
                             `route_name` varchar(255) COMMENT '路由名称（Vue Router 中用于命名路由）',
                             `route_path` varchar(128) COMMENT '路由路径（Vue Router 中定义的 URL 路径）',
                             `component` varchar(128) COMMENT '组件路径（组件页面完整路径，相对于 src/views/，缺省后缀 .vue）',
                             `perm` varchar(128) COMMENT '【按钮】权限标识',
                             `always_show` tinyint DEFAULT 0 COMMENT '【目录】只有一个子路由是否始终显示（1-是 0-否）',
                             `keep_alive` tinyint DEFAULT 0 COMMENT '【菜单】是否开启页面缓存（1-是 0-否）',
                             `visible` tinyint(1) DEFAULT 1 COMMENT '显示状态（1-显示 0-隐藏）',
                             `sort` int DEFAULT 0 COMMENT '排序',
                             `icon` varchar(64) COMMENT '菜单图标',
                             `redirect` varchar(128) COMMENT '跳转路径',
                             `create_time` datetime NULL COMMENT '创建时间',
                             `update_time` datetime NULL COMMENT '更新时间',
                             `params` varchar(255) NULL COMMENT '路由参数',
                             PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '系统菜单表';

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
-- 顶级目录（1-9）：系统/代码生成/文档/接口文档/组件/演示/多级/路由
INSERT INTO `sys_menu` VALUES (1, 0, '0', '系统管理', 'C', '', '/system', 'Layout', NULL, NULL, NULL, 1, 1, 'system', '/system/user', now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2, 0, '0', '代码生成', 'C', '', '/codegen', 'Layout', NULL, NULL, NULL, 1, 2, 'code', '/codegen/index', now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (4, 0, '0', '平台文档', 'C', '', '/doc', 'Layout', NULL, NULL, NULL, 1, 4, 'document', '', now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (5, 0, '0', '接口文档', 'C', '', '/api', 'Layout', NULL, NULL, NULL, 1, 5, 'api', '', now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (6, 0, '0', '组件封装', 'C', '', '/component', 'Layout', NULL, NULL, NULL, 1, 6, 'menu', '', now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (7, 0, '0', '功能演示', 'C', '', '/function', 'Layout', NULL, NULL, NULL, 1, 7, 'menu', '', now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (8, 0, '0', '多级菜单', 'C', NULL, '/multi-level', 'Layout', NULL, 1, NULL, 1, 8, 'cascader', '', now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (9, 0, '0', '路由参数', 'C', '', '/route-param', 'Layout', NULL, NULL, NULL, 1, 9, 'el-icon-ElementPlus', '', now(), now(), NULL);

-- 系统管理
INSERT INTO `sys_menu` VALUES (210, 1, '0,1', '用户管理', 'M', 'User', 'user', 'system/user/index', NULL, NULL, 1, 1, 1, 'el-icon-User', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2101, 210, '0,1,210', '用户查询', 'B', NULL, '', NULL, 'sys:user:list', NULL, NULL, 1, 1, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2102, 210, '0,1,210', '用户新增', 'B', NULL, '', NULL, 'sys:user:create', NULL, NULL, 1, 2, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2103, 210, '0,1,210', '用户编辑', 'B', NULL, '', NULL, 'sys:user:update', NULL, NULL, 1, 3, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2104, 210, '0,1,210', '用户删除', 'B', NULL, '', NULL, 'sys:user:delete', NULL, NULL, 1, 4, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2105, 210, '0,1,210', '重置密码', 'B', NULL, '', NULL, 'sys:user:reset-password', NULL, NULL, 1, 5, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2106, 210, '0,1,210', '用户导入', 'B', NULL, '', NULL, 'sys:user:import', NULL, NULL, 1, 6, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2107, 210, '0,1,210', '用户导出', 'B', NULL, '', NULL, 'sys:user:export', NULL, NULL, 1, 7, '', NULL, now(), now(), NULL);

INSERT INTO `sys_menu` VALUES (220, 1, '0,1', '角色管理', 'M', 'Role', 'role', 'system/role/index', NULL, NULL, 1, 1, 2, 'role', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2201, 220, '0,1,220', '角色查询', 'B', NULL, '', NULL, 'sys:role:list', NULL, NULL, 1, 1, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2202, 220, '0,1,220', '角色新增', 'B', NULL, '', NULL, 'sys:role:create', NULL, NULL, 1, 2, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2203, 220, '0,1,220', '角色编辑', 'B', NULL, '', NULL, 'sys:role:update', NULL, NULL, 1, 3, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2204, 220, '0,1,220', '角色删除', 'B', NULL, '', NULL, 'sys:role:delete', NULL, NULL, 1, 4, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2205, 220, '0,1,220', '角色分配权限', 'B', NULL, '', NULL, 'sys:role:assign', NULL, NULL, 1, 5, '', NULL, now(), now(), NULL);

INSERT INTO `sys_menu` VALUES (230, 1, '0,1', '菜单管理', 'M', 'SysMenu', 'menu', 'system/menu/index', NULL, NULL, 1, 1, 3, 'menu', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2301, 230, '0,1,230', '菜单查询', 'B', NULL, '', NULL, 'sys:menu:list', NULL, NULL, 1, 1, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2302, 230, '0,1,230', '菜单新增', 'B', NULL, '', NULL, 'sys:menu:create', NULL, NULL, 1, 2, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2303, 230, '0,1,230', '菜单编辑', 'B', NULL, '', NULL, 'sys:menu:update', NULL, NULL, 1, 3, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2304, 230, '0,1,230', '菜单删除', 'B', NULL, '', NULL, 'sys:menu:delete', NULL, NULL, 1, 4, '', NULL, now(), now(), NULL);

INSERT INTO `sys_menu` VALUES (240, 1, '0,1', '部门管理', 'M', 'Dept', 'dept', 'system/dept/index', NULL, NULL, 1, 1, 4, 'tree', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2401, 240, '0,1,240', '部门查询', 'B', NULL, '', NULL, 'sys:dept:list', NULL, NULL, 1, 1, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2402, 240, '0,1,240', '部门新增', 'B', NULL, '', NULL, 'sys:dept:create', NULL, NULL, 1, 2, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2403, 240, '0,1,240', '部门编辑', 'B', NULL, '', NULL, 'sys:dept:update', NULL, NULL, 1, 3, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2404, 240, '0,1,240', '部门删除', 'B', NULL, '', NULL, 'sys:dept:delete', NULL, NULL, 1, 4, '', NULL, now(), now(), NULL);

INSERT INTO `sys_menu` VALUES (250, 1, '0,1', '字典管理', 'M', 'Dict', 'dict', 'system/dict/index', NULL, NULL, 1, 1, 5, 'dict', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2501, 250, '0,1,250', '字典查询', 'B', NULL, '', NULL, 'sys:dict:list', NULL, NULL, 1, 1, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2502, 250, '0,1,250', '字典新增', 'B', NULL, '', NULL, 'sys:dict:create', NULL, NULL, 1, 2, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2503, 250, '0,1,250', '字典编辑', 'B', NULL, '', NULL, 'sys:dict:update', NULL, NULL, 1, 3, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2504, 250, '0,1,250', '字典删除', 'B', NULL, '', NULL, 'sys:dict:delete', NULL, NULL, 1, 4, '', NULL, now(), now(), NULL);

INSERT INTO `sys_menu` VALUES (251, 1, '0,1', '字典项', 'M', 'DictItem', 'dict-item', 'system/dict/dict-item', NULL, 0, 1, 0, 6, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2511, 251, '0,1,251', '字典项查询', 'B', NULL, '', NULL, 'sys:dict-item:list', NULL, NULL, 1, 1, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2512, 251, '0,1,251', '字典项新增', 'B', NULL, '', NULL, 'sys:dict-item:create', NULL, NULL, 1, 2, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2513, 251, '0,1,251', '字典项编辑', 'B', NULL, '', NULL, 'sys:dict-item:update', NULL, NULL, 1, 3, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2514, 251, '0,1,251', '字典项删除', 'B', NULL, '', NULL, 'sys:dict-item:delete', NULL, NULL, 1, 4, '', NULL, now(), now(), NULL);

INSERT INTO `sys_menu` VALUES (260, 1, '0,1', '系统日志', 'M', 'Log', 'log', 'system/log/index', NULL, 0, 1, 1, 7, 'document', NULL, now(), now(), NULL);

INSERT INTO `sys_menu` VALUES (270, 1, '0,1', '系统配置', 'M', 'Config', 'config', 'system/config/index', NULL, 0, 1, 1, 8, 'setting', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2701, 270, '0,1,270', '系统配置查询', 'B', NULL, '', NULL, 'sys:config:list', 0, 1, 1, 1, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2702, 270, '0,1,270', '系统配置新增', 'B', NULL, '', NULL, 'sys:config:create', 0, 1, 1, 2, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2703, 270, '0,1,270', '系统配置修改', 'B', NULL, '', NULL, 'sys:config:update', 0, 1, 1, 3, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2704, 270, '0,1,270', '系统配置删除', 'B', NULL, '', NULL, 'sys:config:delete', 0, 1, 1, 4, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2705, 270, '0,1,270', '系统配置刷新', 'B', NULL, '', NULL, 'sys:config:refresh', 0, 1, 1, 5, '', NULL, now(), now(), NULL);

INSERT INTO `sys_menu` VALUES (280, 1, '0,1', '通知公告', 'M', 'Notice', 'notice', 'system/notice/index', NULL, NULL, NULL, 1, 9, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2801, 280, '0,1,280', '通知查询', 'B', NULL, '', NULL, 'sys:notice:list', NULL, NULL, 1, 1, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2802, 280, '0,1,280', '通知新增', 'B', NULL, '', NULL, 'sys:notice:create', NULL, NULL, 1, 2, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2803, 280, '0,1,280', '通知编辑', 'B', NULL, '', NULL, 'sys:notice:update', NULL, NULL, 1, 3, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2804, 280, '0,1,280', '通知删除', 'B', NULL, '', NULL, 'sys:notice:delete', NULL, NULL, 1, 4, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2805, 280, '0,1,280', '通知发布', 'B', NULL, '', NULL, 'sys:notice:publish', 0, 1, 1, 5, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (2806, 280, '0,1,280', '通知撤回', 'B', NULL, '', NULL, 'sys:notice:revoke', 0, 1, 1, 6, '', NULL, now(), now(), NULL);

-- 代码生成
INSERT INTO `sys_menu` VALUES (310, 2, '0,2', '代码生成', 'M', 'Codegen', 'codegen', 'codegen/index', NULL, NULL, 1, 1, 1, 'code', NULL, now(), now(), NULL);

-- 平台文档（外链通过 route_path 识别）
INSERT INTO `sys_menu` VALUES (501, 4, '0,4', '平台文档(外链)', 'M', NULL, 'https://juejin.cn/post/7228990409909108793', '', NULL, NULL, NULL, 1, 1, 'document', '', now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (502, 4, '0,4', '后端文档', 'M', NULL, 'https://youlai.blog.csdn.net/article/details/145178880', '', NULL, NULL, NULL, 1, 2, 'document', '', now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (503, 4, '0,4', '移动端文档', 'M', NULL, 'https://youlai.blog.csdn.net/article/details/143222890', '', NULL, NULL, NULL, 1, 3, 'document', '', now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (504, 4, '0,4', '内部文档', 'M', NULL, 'internal-doc', 'demo/internal-doc', NULL, NULL, NULL, 1, 4, 'document', '', now(), now(), NULL);

-- 接口文档
INSERT INTO `sys_menu` VALUES (601, 5, '0,5', 'Apifox', 'M', 'Apifox', 'apifox', 'demo/api/apifox', NULL, NULL, 1, 1, 1, 'api', '', now(), now(), NULL);

-- 组件封装
INSERT INTO `sys_menu` VALUES (701, 6, '0,6', '富文本编辑器', 'M', 'WangEditor', 'wang-editor', 'demo/wang-editor', NULL, NULL, 1, 1, 2, '', '', now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (702, 6, '0,6', '图片上传', 'M', 'Upload', 'upload', 'demo/upload', NULL, NULL, 1, 1, 3, '', '', now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (703, 6, '0,6', '图标选择器', 'M', 'IconSelect', 'icon-select', 'demo/icon-select', NULL, NULL, 1, 1, 4, '', '', now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (704, 6, '0,6', '字典组件', 'M', 'DictDemo', 'dict-demo', 'demo/dictionary', NULL, NULL, 1, 1, 4, '', '', now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (705, 6, '0,6', '增删改查', 'M', 'Curd', 'curd', 'demo/curd/index', NULL, NULL, 1, 1, 0, '', '', now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (706, 6, '0,6', '列表选择器', 'M', 'TableSelect', 'table-select', 'demo/table-select/index', NULL, NULL, 1, 1, 1, '', '', now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (707, 6, '0,6', '拖拽组件', 'M', 'Drag', 'drag', 'demo/drag', NULL, NULL, NULL, 1, 5, '', '', now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (708, 6, '0,6', '滚动文本', 'M', 'TextScroll', 'text-scroll', 'demo/text-scroll', NULL, NULL, NULL, 1, 6, '', '', now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (709, 6, '0,6', '自适应表格操作列', 'M', 'AutoOperationColumn', 'operation-column', 'demo/auto-operation-column', NULL, NULL, 1, 1, 1, '', '', now(), now(), NULL);

-- 功能演示
INSERT INTO `sys_menu` VALUES (801, 7, '0,7', 'Websocket', 'M', 'WebSocket', '/function/websocket', 'demo/websocket', NULL, NULL, 1, 1, 1, '', '', now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (802, 7, '0,7', 'Icons', 'M', 'IconDemo', 'icon-demo', 'demo/icons', NULL, NULL, 1, 1, 2, 'el-icon-Notification', '', now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (803, 7, '0,7', '字典实时同步', 'M', 'DictSync', 'dict-sync', 'demo/dict-sync', NULL, NULL, NULL, 1, 3, '', '', now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (804, 7, '0,7', 'VxeTable', 'M', 'VxeTable', 'vxe-table', 'demo/vxe-table/index', NULL, NULL, 1, 1, 4, 'el-icon-MagicStick', '', now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (805, 7, '0,7', 'CURD单文件', 'M', 'CurdSingle', 'curd-single', 'demo/curd-single', NULL, NULL, 1, 1, 5, 'el-icon-Reading', '', now(), now(), NULL);

-- 多级菜单示例
INSERT INTO `sys_menu` VALUES (910, 8, '0,8', '菜单一级', 'C', NULL, 'multi-level1', 'Layout', NULL, 1, NULL, 1, 1, '', '', now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (911, 910, '0,8,910', '菜单二级', 'C', NULL, 'multi-level2', 'Layout', NULL, 0, NULL, 1, 1, '', NULL, now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (912, 911, '0,8,910,911', '菜单三级-1', 'M', NULL, 'multi-level3-1', 'demo/multi-level/children/children/level3-1', NULL, 0, 1, 1, 1, '', '', now(), now(), NULL);
INSERT INTO `sys_menu` VALUES (913, 911, '0,8,910,911', '菜单三级-2', 'M', NULL, 'multi-level3-2', 'demo/multi-level/children/children/level3-2', NULL, 0, 1, 1, 2, '', '', now(), now(), NULL);

-- 路由参数
INSERT INTO `sys_menu` VALUES (1001, 9, '0,9', '参数(type=1)', 'M', 'RouteParamType1', 'route-param-type1', 'demo/route-param', NULL, 0, 1, 1, 1, 'el-icon-Star', NULL, now(), now(), '{\"type\": \"1\"}');
INSERT INTO `sys_menu` VALUES (1002, 9, '0,9', '参数(type=2)', 'M', 'RouteParamType2', 'route-param-type2', 'demo/route-param', NULL, 0, 1, 1, 2, 'el-icon-StarFilled', NULL, now(), now(), '{\"type\": \"2\"}');

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
INSERT INTO `sys_role` VALUES (4, '系统管理员1', 'ADMIN1', 4, 1, 1, NULL, now(), NULL, NULL, 0);
INSERT INTO `sys_role` VALUES (5, '系统管理员2', 'ADMIN2', 5, 1, 1, NULL, now(), NULL, NULL, 0);
INSERT INTO `sys_role` VALUES (6, '系统管理员3', 'ADMIN3', 6, 1, 1, NULL, now(), NULL, NULL, 0);
INSERT INTO `sys_role` VALUES (7, '系统管理员4', 'ADMIN4', 7, 1, 1, NULL, now(), NULL, NULL, 0);
INSERT INTO `sys_role` VALUES (8, '系统管理员5', 'ADMIN5', 8, 1, 1, NULL, now(), NULL, NULL, 0);
INSERT INTO `sys_role` VALUES (9, '系统管理员6', 'ADMIN6', 9, 1, 1, NULL, now(), NULL, NULL, 0);
INSERT INTO `sys_role` VALUES (10, '系统管理员7', 'ADMIN7', 10, 1, 1, NULL, now(), NULL, NULL, 0);
INSERT INTO `sys_role` VALUES (11, '系统管理员8', 'ADMIN8', 11, 1, 1, NULL, now(), NULL, NULL, 0);
INSERT INTO `sys_role` VALUES (12, '系统管理员9', 'ADMIN9', 12, 1, 1, NULL, now(), NULL, NULL, 0);

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

-- ============================================
-- 系统管理员角色菜单权限（role_id=2）
-- 顶级目录
INSERT INTO `sys_role_menu` VALUES (2, 1), (2, 2), (2, 4), (2, 5), (2, 6), (2, 7), (2, 8), (2, 9);
-- 系统管理
INSERT INTO `sys_role_menu` VALUES (2, 210), (2, 2101), (2, 2102), (2, 2103), (2, 2104), (2, 2105), (2, 2106), (2, 2107);
INSERT INTO `sys_role_menu` VALUES (2, 220), (2, 2201), (2, 2202), (2, 2203), (2, 2204), (2, 2205);
INSERT INTO `sys_role_menu` VALUES (2, 230), (2, 2301), (2, 2302), (2, 2303), (2, 2304);
INSERT INTO `sys_role_menu` VALUES (2, 240), (2, 2401), (2, 2402), (2, 2403), (2, 2404);
INSERT INTO `sys_role_menu` VALUES (2, 250), (2, 2501), (2, 2502), (2, 2503), (2, 2504);
INSERT INTO `sys_role_menu` VALUES (2, 251), (2, 2511), (2, 2512), (2, 2513), (2, 2514);
INSERT INTO `sys_role_menu` VALUES (2, 260);
INSERT INTO `sys_role_menu` VALUES (2, 270), (2, 2701), (2, 2702), (2, 2703), (2, 2704), (2, 2705);
INSERT INTO `sys_role_menu` VALUES (2, 280), (2, 2801), (2, 2802), (2, 2803), (2, 2804), (2, 2805), (2, 2806);
-- 代码生成
INSERT INTO `sys_role_menu` VALUES (2, 310);
-- 平台文档
INSERT INTO `sys_role_menu` VALUES (2, 501), (2, 502), (2, 503), (2, 504);
-- 接口文档
INSERT INTO `sys_role_menu` VALUES (2, 601);
-- 组件封装
INSERT INTO `sys_role_menu` VALUES (2, 701), (2, 702), (2, 703), (2, 704), (2, 705), (2, 706), (2, 707), (2, 708), (2, 709);
-- 功能演示 / 多级菜单
INSERT INTO `sys_role_menu` VALUES (2, 801), (2, 802), (2, 803), (2, 804), (2, 805), (2, 910), (2, 911), (2, 912), (2, 913);
-- 路由参数
INSERT INTO `sys_role_menu` VALUES (2, 1001), (2, 1002);

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
                             PRIMARY KEY (`id`) USING BTREE,
                             UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '系统用户表';

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'root', '有来技术', 0, '$2a$10$xVWsNOhHrCxh5UbpCE7/HuJ.PAOKcYAqRxD2CO2nVnJS.IAXkr5aq', NULL, 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', '18812345677', 1, 'youlaitech@163.com', now(), NULL, now(), NULL, 0,NULL);
INSERT INTO `sys_user` VALUES (2, 'admin', '系统管理员', 1, '$2a$10$xVWsNOhHrCxh5UbpCE7/HuJ.PAOKcYAqRxD2CO2nVnJS.IAXkr5aq', 1, 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', '18812345678', 1, 'youlaitech@163.com', now(), NULL, now(), NULL, 0,NULL);
INSERT INTO `sys_user` VALUES (3, 'test', '测试小用户', 1, '$2a$10$xVWsNOhHrCxh5UbpCE7/HuJ.PAOKcYAqRxD2CO2nVnJS.IAXkr5aq', 3, 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', '18812345679', 1, 'youlaitech@163.com', now(), NULL, now(), NULL, 0,NULL);

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
INSERT INTO `sys_user_role` VALUES (1, 1);
INSERT INTO `sys_user_role` VALUES (2, 2);
INSERT INTO `sys_user_role` VALUES (3, 3);


-- ----------------------------
-- Table structure for sys_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log` (
                           `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                           `module` varchar(50) NOT NULL COMMENT '日志模块',
                           `request_method` varchar(64) NOT NULL COMMENT '请求方式',
                           `request_params` text COMMENT '请求参数(批量请求参数可能会超过text)',
                           `response_content` mediumtext COMMENT '返回参数',
                           `content` varchar(255) NOT NULL COMMENT '日志内容',
                           `request_uri` varchar(255) COMMENT '请求路径',
                           `method` varchar(255) COMMENT '方法名',
                           `ip` varchar(45) COMMENT 'IP地址',
                           `province` varchar(100) COMMENT '省份',
                           `city` varchar(100) COMMENT '城市',
                           `execution_time` bigint COMMENT '执行时间(ms)',
                           `browser` varchar(100) COMMENT '浏览器',
                           `browser_version` varchar(100) COMMENT '浏览器版本',
                           `os` varchar(100) COMMENT '终端系统',
                           `create_by` bigint COMMENT '创建人ID',
                           `create_time` datetime COMMENT '创建时间',
                           PRIMARY KEY (`id`) USING BTREE,
                           KEY `idx_create_time` (`create_time`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表';

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
                                   `is_read` bigint DEFAULT '0' COMMENT '读取状态（0: 未读, 1: 已读）',
                                   `read_time` datetime COMMENT '阅读时间',
                                   `create_time` datetime NOT NULL COMMENT '创建时间',
                                   `update_time` datetime COMMENT '更新时间',
                                   `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除(0: 未删除, 1: 已删除)',
                                   PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知公告关联表';

INSERT INTO `sys_user_notice` VALUES (1, 1, 2, 1, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (2, 2, 2, 1, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (3, 3, 2, 1, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (4, 4, 2, 1, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (5, 5, 2, 1, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (6, 6, 2, 1, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (7, 7, 2, 1, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (8, 8, 2, 1, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (9, 9, 2, 1, NULL, now(), now(), 0);
INSERT INTO `sys_user_notice` VALUES (10, 10, 2, 1, NULL, now(), now(), 0);
