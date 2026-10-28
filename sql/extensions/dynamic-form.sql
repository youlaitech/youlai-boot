-- 动态表单模块扩展：建表、菜单与示例数据，可重复执行
-- 需先执行 ../youlai-admin.sql
-- Copyright (c) 2026-present, youlai.tech

USE youlai_admin;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 表结构：form_definition
-- ----------------------------
DROP TABLE IF EXISTS `form_definition`;
CREATE TABLE `form_definition` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `form_key` varchar(64) NOT NULL COMMENT '表单唯一标识（业务编码，如 employee_onboarding）',
  `form_name` varchar(100) NOT NULL COMMENT '表单名称',
  `description` varchar(255) DEFAULT NULL COMMENT '表单描述',
  `form_json` json NOT NULL COMMENT '表单规则（form-create rule 数组）',
  `options_json` json DEFAULT NULL COMMENT '表单全局配置（labelPosition/size/submitBtn 等）',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态(0草稿 1已发布 -1已停用)',
  `is_public` tinyint NOT NULL DEFAULT 0 COMMENT '是否允许匿名公开访问(0否 1是)',
  `category` varchar(16) NOT NULL DEFAULT 'normal' COMMENT '表单类型(normal通用表单 workflow审批表单)',
  `menu_id` bigint DEFAULT NULL COMMENT '生成的访问菜单ID（NULL未生成，幂等防重复）',
  `version` int NOT NULL DEFAULT 1 COMMENT '版本号，每次发布+1',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除(0未删 1已删)',
  PRIMARY KEY (`id`),
  KEY `idx_form_key` (`form_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单定义表';

-- ----------------------------
-- 表结构：form_data
-- ----------------------------
DROP TABLE IF EXISTS `form_data`;
CREATE TABLE `form_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `form_id` bigint NOT NULL COMMENT '表单定义ID',
  `form_version` int NOT NULL COMMENT '表单版本（提交时快照）',
  `data_json` json NOT NULL COMMENT '表单数据（field -> value 映射）',
  `create_by` bigint DEFAULT NULL COMMENT '提交人ID（匿名提交为空）',
  `update_by` bigint DEFAULT NULL COMMENT '修改人ID（数据修正时记录）',
  `create_time` datetime NOT NULL COMMENT '提交时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除(0未删 1已删)',
  PRIMARY KEY (`id`),
  KEY `idx_form_id` (`form_id`),
  KEY `idx_create_by` (`create_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单数据表';

-- ----------------------------
-- 表结构：form_snapshot
-- ----------------------------
-- 发布时固化规则，回显按提交时的版本加载，定义变更不影响历史数据
-- 只增不改；update_time 由实体继承 BaseEntity 带入，无业务语义
DROP TABLE IF EXISTS `form_snapshot`;
CREATE TABLE `form_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `form_id` bigint NOT NULL COMMENT '表单定义ID',
  `version` int NOT NULL COMMENT '版本号（发布时递增后的值）',
  `form_json` json NOT NULL COMMENT '表单规则快照（form-create rule 数组）',
  `options_json` json DEFAULT NULL COMMENT '表单全局配置快照',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除(0未删 1已删)',
  PRIMARY KEY (`id`),
  KEY `idx_form_version` (`form_id`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单版本快照表';

-- ----------------------------
-- 菜单（顶级 ID=90；子菜单 route_path 只填当前段，component 指向 views/dynamic-form 下的视图）
-- 顶级菜单 params 中的 badge 是侧边栏角标
-- ----------------------------
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (90, 0, '0', '动态表单', 'C', '', '/dynamic-form', 'Layout', NULL, NULL, 1, 8, 'el-icon-Document', '/dynamic-form/definition', now(), now(), '{\"badge\":\"NEW\"}');
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (9001, 90, '0,90', '表单列表', 'M', 'FormDefinition', 'definition', 'dynamic-form/index', NULL, 1, 1, 1, 'el-icon-List', NULL, now(), now(), NULL);
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (900101, 9001, '0,90,9001', '表单查询', 'B', NULL, '', NULL, 'form:definition:list', NULL, 1, 1, '', NULL, now(), now(), NULL);
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (900102, 9001, '0,90,9001', '表单新增', 'B', NULL, '', NULL, 'form:definition:create', NULL, 1, 2, '', NULL, now(), now(), NULL);
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (900103, 9001, '0,90,9001', '表单修改', 'B', NULL, '', NULL, 'form:definition:update', NULL, 1, 3, '', NULL, now(), now(), NULL);
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (900104, 9001, '0,90,9001', '表单删除', 'B', NULL, '', NULL, 'form:definition:delete', NULL, 1, 4, '', NULL, now(), now(), NULL);
-- 表单设计器：隐藏菜单，列表「设计」按钮带 id 跳转
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (9003, 90, '0,90', '表单设计器', 'M', 'FormDesigner', 'designer', 'dynamic-form/designer', NULL, 1, 0, 3, '', NULL, now(), now(), NULL);
-- 表单预览：隐藏菜单，设计器「预览」按钮带 id 跳转
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (9004, 90, '0,90', '表单预览', 'M', 'FormPreview', 'preview', 'dynamic-form/preview', NULL, 1, 0, 4, '', NULL, now(), now(), NULL);
-- 表单数据：隐藏菜单，列表「数据」按钮带 formKey 跳转
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (9002, 90, '0,90', '表单数据', 'M', 'FormData', 'data', 'dynamic-form/data', NULL, 1, 0, 2, '', NULL, now(), now(), NULL);
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (900201, 9002, '0,90,9002', '数据查询', 'B', NULL, '', NULL, 'form:data:list', NULL, 1, 1, '', NULL, now(), now(), NULL);
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (900202, 9002, '0,90,9002', '数据删除', 'B', NULL, '', NULL, 'form:data:delete', NULL, 1, 2, '', NULL, now(), now(), NULL);

-- ----------------------------
-- 示例表单入口，结构与 saveFormMenu 生成的菜单一致（type=M、component=dynamic-form/render、
-- route_path=formKey、params 带 formKey、route_name 为 FormRender+驼峰 formKey）
-- ----------------------------
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (9005, 90, '0,90', '表单示例', 'C', '', 'sample', 'Layout', NULL, NULL, 1, 5, 'el-icon-Files', '/dynamic-form/sample/open_source_survey', now(), now(), NULL);
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (900501, 9005, '0,90,9005', '开源项目问卷收集', 'M', 'FormRenderOpenSourceSurvey', 'open_source_survey', 'dynamic-form/render', NULL, NULL, 1, 1, 'el-icon-Tickets', NULL, now(), now(), '{"formKey":"open_source_survey"}');
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (900502, 9005, '0,90,9005', '活动报名表', 'M', 'FormRenderActivityRegistration', 'activity_registration', 'dynamic-form/render', NULL, NULL, 1, 2, 'el-icon-Calendar', NULL, now(), now(), '{"formKey":"activity_registration"}');
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `tree_path`, `name`, `type`, `route_name`, `route_path`, `component`, `perm`, `keep_alive`, `visible`, `sort`, `icon`, `redirect`, `create_time`, `update_time`, `params`) VALUES (900503, 9005, '0,90,9005', '意见反馈', 'M', 'FormRenderFeedback', 'feedback', 'dynamic-form/render', NULL, NULL, 1, 3, 'el-icon-ChatLineRound', NULL, now(), now(), '{"formKey":"feedback"}');

-- ----------------------------
-- 系统管理员角色授权（role_id=2）
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` VALUES (2, 90), (2, 9001), (2, 900101), (2, 900102), (2, 900103), (2, 900104), (2, 9002), (2, 900201), (2, 900202), (2, 9003), (2, 9004), (2, 9005), (2, 900501), (2, 900502), (2, 900503);

-- ----------------------------
-- 示例表单：已发布，同时挂了内嵌菜单（900501-900503）与匿名外链 /f/{form_key}
-- ID 固定 1001-1003，避开用户自建的自增段
-- ----------------------------
INSERT IGNORE INTO `form_definition` (`id`, `form_key`, `form_name`, `description`, `form_json`, `options_json`, `status`, `is_public`, `menu_id`, `version`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`) VALUES
(1001, 'open_source_survey', '开源项目问卷收集', '面向社区的开源项目推荐与信息收集问卷', '[{"type":"input","field":"project_name","title":"项目名称","info":"","props":{"placeholder":"如 vue3-element-admin"},"validate":[{"required":true,"message":"请输入项目名称"}]},{"type":"input","field":"repo_url","title":"项目地址","info":"","props":{"placeholder":"https://github.com/xxx/xxx"},"validate":[{"required":true,"message":"请输入项目地址"}]},{"type":"select","field":"category","title":"项目领域","info":"","props":{},"options":[{"label":"前端","value":"frontend"},{"label":"后端","value":"backend"},{"label":"移动端","value":"mobile"},{"label":"AI 与大模型","value":"ai"},{"label":"开发工具","value":"tools"},{"label":"其他","value":"other"}],"validate":[{"required":true,"message":"请选择项目领域"}]},{"type":"radio","field":"license","title":"开源协议","info":"","options":[{"label":"MIT","value":"MIT"},{"label":"Apache-2.0","value":"Apache-2.0"},{"label":"GPLv3","value":"GPLv3"},{"label":"其他","value":"other"}]},{"type":"select","field":"stars","title":"Star 数量级","info":"","props":{},"options":[{"label":"100 以内","value":"lt100"},{"label":"100 ~ 1k","value":"100to1k"},{"label":"1k ~ 10k","value":"1kto10k"},{"label":"10k 以上","value":"gt10k"}]},{"type":"textarea","field":"recommend","title":"推荐理由","info":"","props":{"placeholder":"一句话说说它解决了什么问题"},"validate":[{"required":true,"message":"请填写推荐理由"}]},{"type":"input","field":"contact","title":"联系邮箱","info":"","props":{"placeholder":"选填，便于回访"}}]', '{"labelPosition":"right","size":"default"}', 1, 1, 900501, 1, now(), now(), NULL, NULL, 0),
(1002, 'activity_registration', '活动报名表', '线下活动/聚会报名登记，含场次选择与用餐需求', '[{"type":"input","field":"name","title":"姓名","info":"","props":{},"validate":[{"required":true,"message":"请输入姓名"}]},{"type":"input","field":"phone","title":"手机号码","info":"","props":{"placeholder":"用于接收活动通知"},"validate":[{"required":true,"message":"请输入手机号码"}]},{"type":"radio","field":"gender","title":"性别","info":"","options":[{"label":"男","value":"male"},{"label":"女","value":"female"}]},{"type":"checkbox","field":"sessions","title":"参加场次","info":"","options":[{"label":"主论坛","value":"main"},{"label":"工作坊 A","value":"workshop_a"},{"label":"工作坊 B","value":"workshop_b"},{"label":"晚间交流","value":"evening"}]},{"type":"inputNumber","field":"attendees","title":"同行人数","info":"","props":{"min":0,"max":10}},{"type":"radio","field":"dinner","title":"用餐需求","info":"","options":[{"label":"正常","value":"normal"},{"label":"素食","value":"vegetarian"},{"label":"无需用餐","value":"none"}]},{"type":"textarea","field":"remark","title":"备注","info":"","props":{"placeholder":"选填"}}]', '{"labelPosition":"right","size":"default"}', 1, 1, 900502, 1, now(), now(), NULL, NULL, 0),
(1003, 'feedback', '意见反馈', '产品/系统通用意见反馈表，含满意度评分', '[{"type":"select","field":"type","title":"反馈类型","info":"","props":{},"options":[{"label":"功能建议","value":"feature"},{"label":"问题报告","value":"bug"},{"label":"体验吐槽","value":"ux"},{"label":"其他","value":"other"}],"validate":[{"required":true,"message":"请选择反馈类型"}]},{"type":"rate","field":"rating","title":"整体满意度","info":"","props":{}},{"type":"textarea","field":"content","title":"反馈内容","info":"","props":{"placeholder":"请具体描述你的建议或遇到的问题"},"validate":[{"required":true,"message":"请填写反馈内容"}]},{"type":"input","field":"contact","title":"联系方式","info":"","props":{"placeholder":"选填，便于跟进回复"}}]', '{"labelPosition":"right","size":"default"}', 1, 1, 900503, 1, now(), now(), NULL, NULL, 0);

-- 示例表单的 v1 快照
INSERT IGNORE INTO `form_snapshot` (`id`, `form_id`, `version`, `form_json`, `options_json`, `create_time`, `update_time`, `is_deleted`) VALUES
(1001, 1001, 1, (SELECT `form_json` FROM `form_definition` WHERE `id` = 1001), (SELECT `options_json` FROM `form_definition` WHERE `id` = 1001), now(), now(), 0),
(1002, 1002, 1, (SELECT `form_json` FROM `form_definition` WHERE `id` = 1002), (SELECT `options_json` FROM `form_definition` WHERE `id` = 1002), now(), now(), 0),
(1003, 1003, 1, (SELECT `form_json` FROM `form_definition` WHERE `id` = 1003), (SELECT `options_json` FROM `form_definition` WHERE `id` = 1003), now(), now(), 0);

