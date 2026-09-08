package com.youlai.boot.module.workflow.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 审批记录VO（已办用户任务 + 审批意见）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Schema(description = "审批记录对象")
@Getter
@Setter
public class ApprovalHistoryVO {

    @Schema(description = "节点ID（与流程走向环节 nodeId 对应，供前端按环节归位记录）")
    private String activityId;

    @Schema(description = "节点名称")
    private String activityName;

    @Schema(description = "办理人（用户名）")
    private String assignee;

    @Schema(description = "审批意见（无则为空）")
    private String comment;

    @Schema(description = "任务开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    @Schema(description = "任务办理时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

}
