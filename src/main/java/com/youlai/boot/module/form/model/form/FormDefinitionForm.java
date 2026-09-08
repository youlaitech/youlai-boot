package com.youlai.boot.module.form.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 表单定义表单对象
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Schema(description = "表单定义")
@Data
public class FormDefinitionForm {

    @Schema(description = "表单ID", example = "1")
    private Long id;

    @Schema(description = "表单唯一标识（创建后不可修改）", example = "employee_onboarding")
    @NotBlank(message = "表单标识不能为空")
    @Size(max = 64, message = "表单标识长度不能超过64个字符")
    @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "表单标识须以小写字母开头，仅含小写字母、数字、下划线")
    private String formKey;

    @Schema(description = "表单名称", example = "新员工入职信息采集")
    @NotBlank(message = "表单名称不能为空")
    @Size(max = 100, message = "表单名称长度不能超过100个字符")
    private String formName;

    @Schema(description = "表单描述")
    @Size(max = 255, message = "表单描述长度不能超过255个字符")
    private String description;

    @Schema(description = "表单规则（form-create rule 数组 JSON 字符串，由设计器产出）")
    private String formJson;

    @Schema(description = "表单全局配置 JSON 字符串")
    private String optionsJson;

    @Schema(description = "是否允许匿名公开访问(0否 1是)，公开访问总开关")
    private Integer isPublic;

    @Schema(description = "表单类型(normal通用表单 workflow工作流表单，缺省normal)", example = "normal")
    @Pattern(regexp = "^(normal|workflow)$", message = "表单类型不合法")
    private String category;

}
