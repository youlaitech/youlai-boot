package com.youlai.boot.module.form.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 表单菜单发布表单对象（发布向导"入口配置"步骤提交）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Schema(description = "表单菜单发布对象")
@Data
public class FormMenuForm {

    @Schema(description = "菜单名称（侧边栏展示）", example = "入职信息采集")
    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 50, message = "菜单名称长度不能超过50个字符")
    private String menuName;

    @Schema(description = "上级菜单ID（须为目录类型；为空时自动使用/创建默认目录\"表单中心\"）", example = "11")
    private Long parentId;

    @Schema(description = "可见角色ID列表（ROOT 角色无需授权，天然全量可见）")
    private List<Long> roleIds;

}
