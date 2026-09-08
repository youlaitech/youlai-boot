package com.youlai.boot.module.workflow.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 流程审批阶段VO（发起页与审批弹窗的流程走向预览）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Schema(description = "流程审批阶段对象")
@Getter
@Builder
public class ProcessStageVO {

    @Schema(description = "节点ID")
    private final String nodeId;

    @Schema(description = "节点名称（审批环节）")
    private final String nodeName;

    @Schema(description = "审批角色名称列表（角色候选组任务）")
    private final List<String> roleNames;

    @Schema(description = "角色成员账号列表（演示环境直观展示哪个用户办理该环节）")
    private final List<String> userNames;

    @Schema(description = "是否发起人本人办理（assignee=${initiator}）")
    private final Boolean initiator;

}
