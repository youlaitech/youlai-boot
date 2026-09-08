package com.youlai.boot.module.form.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 表单菜单配置VO（发布向导回显已生成的菜单）
 *
 * @author Ray.Hao
 * @since 4.9.0
 */
@Schema(description = "表单菜单配置对象")
@Getter
@Setter
public class FormMenuVO {

    @Schema(description = "菜单ID")
    private Long menuId;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "上级菜单ID")
    private Long parentId;

    @Schema(description = "可见角色ID列表")
    private List<Long> roleIds;

}
