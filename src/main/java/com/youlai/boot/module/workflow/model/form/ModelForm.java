package com.youlai.boot.module.workflow.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 流程模型表单对象
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Schema(description = "流程模型")
@Data
public class ModelForm {

    @Schema(description = "模型名称", example = "请假审批")
    @NotBlank(message = "模型名称不能为空")
    @Size(max = 100, message = "模型名称长度不能超过100个字符")
    private String name;

    @Schema(description = "模型标识（创建后不可修改，部署时作为流程定义 key）", example = "leave")
    @NotBlank(message = "模型标识不能为空")
    @Size(max = 64, message = "模型标识长度不能超过64个字符")
    @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "模型标识须以小写字母开头，仅含小写字母、数字、下划线")
    private String key;

    @Schema(description = "模型描述")
    @Size(max = 255, message = "模型描述长度不能超过255个字符")
    private String description;

}
