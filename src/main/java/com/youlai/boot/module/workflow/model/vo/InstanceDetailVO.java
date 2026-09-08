package com.youlai.boot.module.workflow.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.youlai.boot.module.workflow.enums.InstanceStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

/**
 * 流程实例详情VO（详情页渲染：发起表单回显 + 审批记录）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Schema(description = "流程实例详情对象")
@Getter
@Setter
public class InstanceDetailVO {

    @Schema(description = "流程实例ID")
    private String id;

    @Schema(description = "流程定义名称")
    private String processName;

    @Schema(description = "发起人（用户名）")
    private String startUser;

    @Schema(description = "绑定表单标识（未绑定表单为空）")
    private String formKey;

    @Schema(description = "发起表单规则（按提交时快照严格回显）")
    private String formJson;

    @Schema(description = "发起表单全局配置（快照）")
    private String optionsJson;

    @Schema(description = "发起表单数据（field -> value 映射的 JSON 字符串）")
    private String dataJson;

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

    @Schema(description = "审批记录（按时间正序）")
    private List<ApprovalHistoryVO> history;

    @Schema(description = "流程走向预览（按 BPMN 编排顺序，展示各环节审批角色与办理人）")
    private List<ProcessStageVO> stages;

    @Schema(description = "当前进行中的环节下标（正常结束为环节总数即全部完成；终止/撤销为最后办完环节的下一位置）")
    private Integer activeStageIndex;

}
