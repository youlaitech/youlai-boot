package com.youlai.boot.system.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 推断菜单配置入参
 *
 * @author Ray.Hao
 * @since 0.1.0
 */
@Schema(description = "AI 推断菜单配置入参")
@Data
public class MenuAiFillForm {

    @Schema(description = "菜单名称", example = "公告管理")
    @NotBlank(message = "菜单名称不能为空")
    private String name;

    @Schema(description = "菜单类型（C-目录 M-菜单 E-外链 B-按钮）", example = "M")
    private String type;

    @Schema(description = "上级菜单ID，用于参考同级菜单的命名风格", example = "1")
    private Long parentId;
}
