package com.youlai.boot.module.workflow.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 审批任务表单对象（通过）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Schema(description = "审批任务表单对象")
@Data
public class CompleteTaskForm {

    @Schema(description = "审批意见（写入引擎评论，随任务历史归档）")
    @Size(max = 500, message = "审批意见长度不能超过500个字符")
    private String comment;

    @Schema(description = "流程变量（驱动后续网关条件）")
    private Map<String, Object> variables;

}
