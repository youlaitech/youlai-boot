package com.youlai.boot.module.form.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 生成表单入参
 *
 * @author Ray.Hao
 * @since 0.1.0
 */
@Schema(description = "AI 生成表单入参")
@Data
public class FormAiGenerateForm {

    @Schema(description = "表单需求描述", example = "请假申请：姓名、请假类型、起止日期、请假事由（必填）、附件")
    @NotBlank(message = "需求描述不能为空")
    private String description;
}
