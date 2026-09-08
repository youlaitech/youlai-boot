package com.youlai.boot.module.workflow.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.youlai.boot.module.workflow.enums.InstanceStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 流程实例VO（我的流程分页列表）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Schema(description = "流程实例对象")
@Getter
@Setter
public class InstancePageVO {

    @Schema(description = "流程实例ID")
    private String id;

    @Schema(description = "流程定义标识")
    private String processDefinitionKey;

    @Schema(description = "流程定义名称")
    private String processName;

    @Schema(description = "业务主键（绑定的表单数据ID，未绑定表单为空）")
    private String businessKey;

    @Schema(description = "绑定表单标识（未绑定为空）")
    private String formKey;

    @Schema(description = "实例状态")
    private InstanceStatusEnum status;

    @Schema(description = "终止原因（status=terminated 时有值）")
    private String deleteReason;

    @Schema(description = "发起时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    @Schema(description = "结束时间（运行中为空）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

}
