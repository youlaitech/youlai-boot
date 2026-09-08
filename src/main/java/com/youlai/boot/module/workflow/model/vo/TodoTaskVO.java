package com.youlai.boot.module.workflow.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 待办任务VO（待办分页列表）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Schema(description = "待办任务对象")
@Getter
@Setter
public class TodoTaskVO {

    @Schema(description = "任务ID")
    private String id;

    @Schema(description = "任务名称")
    private String name;

    @Schema(description = "流程实例ID")
    private String processInstanceId;

    @Schema(description = "流程定义名称")
    private String processName;

    @Schema(description = "业务主键（绑定的表单数据ID，未绑定表单为空）")
    private String businessKey;

    @Schema(description = "绑定表单标识（未绑定为空）")
    private String formKey;

    @Schema(description = "任务创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

}
