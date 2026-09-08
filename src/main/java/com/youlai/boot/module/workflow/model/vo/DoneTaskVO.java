package com.youlai.boot.module.workflow.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 已办任务VO（已办分页列表）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Schema(description = "已办任务对象")
@Getter
@Setter
public class DoneTaskVO {

    @Schema(description = "任务ID")
    private String id;

    @Schema(description = "任务名称")
    private String name;

    @Schema(description = "流程实例ID")
    private String processInstanceId;

    @Schema(description = "流程定义名称")
    private String processName;

    @Schema(description = "任务办理时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    @Schema(description = "处理意见")
    private String comment;

}
