# 工作流模块（可选扩展）
# Copyright (c) 2021-present, youlai.tech

-- ----------------------------------------------------------------------------
-- 工作流模块增量脚本
-- 前置：先执行 ../youlai-admin.sql 与 dynamic-form.sql
-- 引擎表（ACT_*）由 Flowable 按 flowable.database-schema-update 策略自动创建，本脚本不含建表
-- ----------------------------------------------------------------------------

USE youlai_admin;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 菜单（顶级 ID=8，顺延动态表单 ID=7）
-- ----------------------------
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (8, 0, '0', '工作流', 'C', '', '/workflow', 'Layout', NULL, 1, NULL, 1, 8, 'el-icon-Connection', '/workflow/launch', now(), now(), '{\"badge\":\"NEW\"}');
UPDATE `sys_menu` SET redirect = '/workflow/launch', update_time = now() WHERE id = 8 AND redirect = '/workflow/model';
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (801, 8, '0,8', '流程设计', 'M', 'WorkflowModel', 'model', 'workflow/model', NULL, NULL, NULL, 1, 1, 'el-icon-Coin', NULL, now(), now(), NULL);
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (80101, 801, '0,8,801', '流程查询', 'B', NULL, '', NULL, 'workflow:model:list', NULL, NULL, 1, 1, '', NULL, now(), now(), NULL);
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (80102, 801, '0,8,801', '流程新增', 'B', NULL, '', NULL, 'workflow:model:create', NULL, NULL, 1, 2, '', NULL, now(), now(), NULL);
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (80103, 801, '0,8,801', '流程修改', 'B', NULL, '', NULL, 'workflow:model:update', NULL, NULL, 1, 3, '', NULL, now(), now(), NULL);
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (80104, 801, '0,8,801', '流程删除', 'B', NULL, '', NULL, 'workflow:model:delete', NULL, NULL, 1, 4, '', NULL, now(), now(), NULL);
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (80105, 801, '0,8,801', '流程发布', 'B', NULL, '', NULL, 'workflow:model:deploy', NULL, NULL, 1, 5, '', NULL, now(), now(), NULL);
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (80106, 801, '0,8,801', '流程重置', 'B', NULL, '', NULL, 'workflow:model:reset', NULL, NULL, 1, 6, '', NULL, now(), now(), NULL);
-- 流程启停：启用开关走 definition:update，删除复用 model:delete
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (80107, 801, '0,8,801', '流程启停', 'B', NULL, '', NULL, 'workflow:definition:update', NULL, NULL, 1, 7, '', NULL, now(), now(), NULL);
-- 流程设计器（隐藏菜单，列表"设计"按钮跳转）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (802, 8, '0,8', '流程设计器', 'M', 'WorkflowDesigner', 'designer', 'workflow/designer', NULL, NULL, 1, 0, 2, '', NULL, now(), now(), NULL);

-- ----------------------------
-- 存量环境迁移（幂等可重复执行）
-- 1) 菜单重命名：INSERT IGNORE 不覆盖已有行，改名需显式 UPDATE
-- 2) 「流程发布」并入「流程设计」，删除 803 系菜单及角色关联
-- ----------------------------
UPDATE `sys_menu` SET name = '流程设计', update_time = now() WHERE id = 801 AND name = '流程模型';
UPDATE `sys_menu` SET name = '流程查询', update_time = now() WHERE id = 80101 AND name = '模型查询';
UPDATE `sys_menu` SET name = '流程新增', update_time = now() WHERE id = 80102 AND name = '模型新增';
UPDATE `sys_menu` SET name = '流程修改', update_time = now() WHERE id = 80103 AND name = '模型修改';
UPDATE `sys_menu` SET name = '流程删除', update_time = now() WHERE id = 80104 AND name = '模型删除';
UPDATE `sys_menu` SET name = '流程发布', update_time = now() WHERE id = 80105 AND name IN ('模型部署', '流程部署');
UPDATE `sys_menu` SET name = '流程重置', update_time = now() WHERE id = 80106 AND name = '模型重置';
UPDATE `sys_menu` SET name = '流程启停', update_time = now() WHERE id = 80107 AND name IN ('发布停用', '流程停用');
-- 先迁移 803 授权到 80107 再删菜单，避免角色丢失启停能力
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 80107 FROM `sys_role_menu` rm WHERE rm.menu_id = 80302;
DELETE FROM `sys_role_menu` WHERE menu_id IN (803, 80301, 80302, 80303, 80108);
DELETE FROM `sys_menu` WHERE id IN (803, 80301, 80302, 80303, 80108);
-- 发起/审批中心仅登录校验，不设按钮权限
-- 审批中心 = 待办/已办/我发起 三个页签
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (804, 8, '0,8', '发起流程', 'M', 'WorkflowLaunch', 'launch', 'workflow/launch', NULL, NULL, NULL, 1, 4, 'el-icon-Promotion', NULL, now(), now(), NULL);
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (805, 8, '0,8', '审批中心', 'M', 'WorkflowTask', 'task', 'workflow/task/index', NULL, NULL, NULL, 1, 5, 'el-icon-Stamp', NULL, now(), now(), NULL);
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `always_show`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (80501, 805, '0,8,805', '终止流程', 'B', NULL, '', NULL, 'workflow:instance:terminate', NULL, NULL, 1, 1, '', NULL, now(), now(), NULL);
-- 存量环境迁移：805 升级为「审批中心」，806/807 并入 805
UPDATE `sys_menu` SET name = '审批中心', route_name = 'WorkflowTask', route_path = 'task', component = 'workflow/task/index', icon = 'el-icon-Stamp', update_time = now() WHERE id = 805 AND component = 'workflow/instance';
DELETE FROM `sys_role_menu` WHERE menu_id IN (806, 807);
DELETE FROM `sys_menu` WHERE id IN (806, 807);

-- ----------------------------
-- 系统管理员角色授权（role_id=2）
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` VALUES (2, 8), (2, 801), (2, 80101), (2, 80102), (2, 80103), (2, 80104), (2, 80105), (2, 80106), (2, 80107), (2, 802), (2, 804), (2, 805), (2, 80501);

-- ----------------------------
-- 演示角色与用户（ID 段 8-10，顺延 youlai-admin.sql 预置 1-7；密码 123456）
-- 审批环节按 BPMN candidateGroups 绑定专用角色，不依赖 ADMIN
-- ----------------------------
INSERT IGNORE INTO `sys_role` VALUES (8, '总经理', 'GENERAL_MANAGER', 8, 1, 4, NULL, now(), NULL, now(), 0);
INSERT IGNORE INTO `sys_role` VALUES (9, '财务', 'FINANCE', 9, 1, 4, NULL, now(), NULL, now(), 0);
INSERT IGNORE INTO `sys_role` VALUES (10, '行政', 'ADMINISTRATION', 10, 1, 4, NULL, now(), NULL, now(), 0);

INSERT IGNORE INTO `sys_user` VALUES (8, 'manager', '总经理', 1, '$2a$10$xVWsNOhHrCxh5UbpCE7/HuJ.PAOKcYAqRxD2CO2nVnJS.IAXkr5aq', 1, 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', '18812345684', 1, 'manager@youlaitech.com', now(), NULL, now(), NULL, 0);
INSERT IGNORE INTO `sys_user` VALUES (9, 'finance', '财务专员', 2, '$2a$10$xVWsNOhHrCxh5UbpCE7/HuJ.PAOKcYAqRxD2CO2nVnJS.IAXkr5aq', 1, 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', '18812345685', 1, 'finance@youlaitech.com', now(), NULL, now(), NULL, 0);
INSERT IGNORE INTO `sys_user` VALUES (10, 'clerk', '行政专员', 2, '$2a$10$xVWsNOhHrCxh5UbpCE7/HuJ.PAOKcYAqRxD2CO2nVnJS.IAXkr5aq', 1, 'https://foruda.gitee.com/images/1723603502796844527/03cdca2a_716974.gif', '18812345686', 1, 'clerk@youlaitech.com', now(), NULL, now(), NULL, 0);

INSERT IGNORE INTO `sys_user_role` VALUES (8, 8), (9, 9), (10, 10);

-- ----------------------------
-- 演示角色的工作流菜单授权（不含流程设计等管理菜单）
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` VALUES (4, 8), (4, 804), (4, 805);
INSERT IGNORE INTO `sys_role_menu` VALUES (5, 8), (5, 804), (5, 805);
INSERT IGNORE INTO `sys_role_menu` VALUES (6, 8), (6, 804), (6, 805);
INSERT IGNORE INTO `sys_role_menu` VALUES (8, 8), (8, 804), (8, 805);
INSERT IGNORE INTO `sys_role_menu` VALUES (9, 8), (9, 804), (9, 805);
INSERT IGNORE INTO `sys_role_menu` VALUES (10, 8), (10, 804), (10, 805);

-- ----------------------------
-- 演示流程的发起表单（依赖动态表单扩展）
-- form_key 与 BPMN 开始节点 flowable:formKey 一一对应
-- ID 段 1004-1007 顺延 dynamic-form.sql 预置的 1001-1003
-- ----------------------------
INSERT IGNORE INTO `form_definition` (`id`, `form_key`, `form_name`, `description`, `form_json`, `options_json`, `status`, `is_public`, `category`, `menu_id`, `version`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES
(1004, 'leave_apply', '请假申请', '演示流程"请假审批"的发起表单，天数超过 3 天自动流转总经理审批', '[{"type":"select","field":"leave_type","title":"请假类型","info":"","props":{},"options":[{"label":"事假","value":"personal"},{"label":"病假","value":"sick"},{"label":"年假","value":"annual"},{"label":"调休","value":"compensatory"}],"validate":[{"required":true,"message":"请选择请假类型"}]},{"type":"inputNumber","field":"days","title":"请假天数","info":"","props":{"min":0.5,"max":30,"precision":1},"validate":[{"required":true,"message":"请输入请假天数"}]},{"type":"datePicker","field":"start_date","title":"开始日期","info":"","props":{"valueFormat":"YYYY-MM-DD"},"validate":[{"required":true,"message":"请选择开始日期"}]},{"type":"datePicker","field":"end_date","title":"结束日期","info":"","props":{"valueFormat":"YYYY-MM-DD"},"validate":[{"required":true,"message":"请选择结束日期"}]},{"type":"textarea","field":"reason","title":"请假事由","info":"","props":{"placeholder":"请简要说明请假事由"},"validate":[{"required":true,"message":"请填写请假事由"}]}]', '{"labelPosition":"right","size":"default"}', 1, 0, 'workflow', NULL, 1, now(), now(), NULL, NULL, 0),
(1005, 'expense_apply', '报销申请', '演示流程"报销审批"的发起表单，金额超过 5000 元自动流转总经理审批', '[{"type":"select","field":"expense_type","title":"报销类型","info":"","props":{},"options":[{"label":"差旅费","value":"travel"},{"label":"办公费","value":"office"},{"label":"招待费","value":"hospitality"},{"label":"培训费","value":"training"}],"validate":[{"required":true,"message":"请选择报销类型"}]},{"type":"inputNumber","field":"amount","title":"报销金额（元）","info":"","props":{"min":0,"max":1000000,"precision":2},"validate":[{"required":true,"message":"请输入报销金额"}]},{"type":"datePicker","field":"expense_date","title":"费用发生日期","info":"","props":{"valueFormat":"YYYY-MM-DD"},"validate":[{"required":true,"message":"请选择费用发生日期"}]},{"type":"textarea","field":"reason","title":"报销说明","info":"","props":{"placeholder":"请简要说明费用用途"},"validate":[{"required":true,"message":"请填写报销说明"}]}]', '{"labelPosition":"right","size":"default"}', 1, 0, 'workflow', NULL, 1, now(), now(), NULL, NULL, 0),
(1006, 'purchase_apply', '采购申请', '演示流程"采购申请"的发起表单，金额超过 1 万元流转总经理审批，通过后回退发起人执行采购', '[{"type":"input","field":"item_name","title":"采购物品","info":"","props":{"placeholder":"请输入采购物品名称"},"validate":[{"required":true,"message":"请填写采购物品"}]},{"type":"inputNumber","field":"quantity","title":"采购数量","info":"","props":{"min":1,"precision":0},"validate":[{"required":true,"message":"请输入采购数量"}]},{"type":"inputNumber","field":"amount","title":"预估金额（元）","info":"","props":{"min":0,"precision":2},"validate":[{"required":true,"message":"请输入预估金额"}]},{"type":"textarea","field":"reason","title":"采购事由","info":"","props":{"placeholder":"请简要说明采购背景与用途"},"validate":[{"required":true,"message":"请填写采购事由"}]}]', '{"labelPosition":"right","size":"default"}', 1, 0, 'workflow', NULL, 1, now(), now(), NULL, NULL, 0),
(1007, 'seal_apply', '用章申请', '演示流程"用章申请"的发起表单，部门领导与行政两级审批', '[{"type":"select","field":"seal_type","title":"印章类型","info":"","props":{},"options":[{"label":"公章","value":"official"},{"label":"合同章","value":"contract"},{"label":"财务章","value":"finance"},{"label":"法人章","value":"legal"}],"validate":[{"required":true,"message":"请选择印章类型"}]},{"type":"datePicker","field":"usage_date","title":"用章日期","info":"","props":{"valueFormat":"YYYY-MM-DD"},"validate":[{"required":true,"message":"请选择用章日期"}]},{"type":"textarea","field":"usage","title":"用章事由","info":"","props":{"placeholder":"请说明用章文件及事由"},"validate":[{"required":true,"message":"请填写用章事由"}]}]', '{"labelPosition":"right","size":"default"}', 1, 0, 'workflow', NULL, 1, now(), now(), NULL, NULL, 0);

-- 按 form_key 幂等纠正 category，确保设计器能检索到全部演示表单
UPDATE `form_definition` SET category = 'workflow'
WHERE form_key IN ('leave_apply', 'expense_apply', 'purchase_apply', 'seal_apply')
  AND category <> 'workflow';

-- 预置表单版本快照（固化 v1 规则，防止编辑定义后历史数据回显漂移）
INSERT IGNORE INTO `form_snapshot` (`id`, `form_id`, `version`, `form_json`, `options_json`, `create_time`, `update_time`, `is_deleted`) VALUES
(1004, 1004, 1, (SELECT `form_json` FROM `form_definition` WHERE `id` = 1004), (SELECT `options_json` FROM `form_definition` WHERE `id` = 1004), now(), now(), 0),
(1005, 1005, 1, (SELECT `form_json` FROM `form_definition` WHERE `id` = 1005), (SELECT `options_json` FROM `form_definition` WHERE `id` = 1005), now(), now(), 0),
(1006, 1006, 1, (SELECT `form_json` FROM `form_definition` WHERE `id` = 1006), (SELECT `options_json` FROM `form_definition` WHERE `id` = 1006), now(), now(), 0),
(1007, 1007, 1, (SELECT `form_json` FROM `form_definition` WHERE `id` = 1007), (SELECT `options_json` FROM `form_definition` WHERE `id` = 1007), now(), now(), 0);
