package com.youlai.boot.module.workflow.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 待办任务详情VO（审批页渲染：发起表单只读回显 + 审批记录）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Schema(description = "待办任务详情对象")
@Getter
@Setter
public class TaskDetailVO {

    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "当前任务节点ID（BPMN 节点，用于流程走向高亮定位）")
    private String taskDefinitionKey;

    @Schema(description = "流程实例ID")
    private String processInstanceId;

    @Schema(description = "流程定义名称")
    private String processName;

    @Schema(description = "发起表单标识（未绑定表单为空）")
    private String formKey;

    @Schema(description = "发起表单规则（按提交时快照严格回显）")
    private String formJson;

    @Schema(description = "发起表单全局配置（快照）")
    private String optionsJson;

    @Schema(description = "发起表单数据（field -> value 映射的 JSON 字符串）")
    private String dataJson;

    @Schema(description = "当前任务绑定表单标识（任务节点 formKey，未绑定为空）")
    private String taskFormKey;

    @Schema(description = "审批记录（按时间正序）")
    private List<ApprovalHistoryVO> history;

    @Schema(description = "流程走向预览（按 BPMN 编排顺序，展示各环节审批角色与办理人）")
    private List<ProcessStageVO> stages;

}
