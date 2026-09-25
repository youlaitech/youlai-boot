package com.youlai.boot.system.model.form;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

/**
 * 字典项表单对象
 *
 * @author Ray Hao
 * @since 2.9.0
 */
@Schema(description = "字典项表单")
@Data
public class DictItemForm {

    @Schema(description = "字典项ID（由请求路径传入并覆盖，仅用于表单回显）")
    private Long id;

    @Schema(description = "字典编码（由请求路径传入并覆盖，仅用于表单回显）")
    private String dictCode;

    @Schema(description = "字典项值")
    @NotBlank(message = "字典项值不能为空")
    @Length(max = 100, message = "字典项值长度不能超过 100 个字符")
    private String value;

    @Schema(description = "字典项标签")
    @NotBlank(message = "字典项标签不能为空")
    @Length(max = 100, message = "字典项标签长度不能超过 100 个字符")
    private String label;

    @Schema(description = "排序")
    @Range(min = 0, max = 9999, message = "排序值需在 0-9999 之间")
    private Integer sort;

    @Schema(description = "状态（1-启用，0-禁用）")
    @Range(min = 0, max = 1, message = "状态取值不正确")
    private Integer status;

    @Schema(description = "标签样式（primary-主要 success-成功 warning-警告 danger-危险 info-信息）")
    @Size(max = 30, message = "标签样式长度不能超过 30 个字符")
    private String tagType;

}
