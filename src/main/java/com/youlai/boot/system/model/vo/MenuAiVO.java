package com.youlai.boot.system.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * AI 推断的菜单配置
 *
 * @author Ray.Hao
 * @since 0.1.0
 */
@Schema(description = "AI 推断的菜单配置")
@Data
public class MenuAiVO {

    @Schema(description = "访问路径")
    private String routePath;

    @Schema(description = "权限标识")
    private String perm;

    @Schema(description = "建议的图标关键词，按贴切程度排列")
    private List<String> iconKeywords;
}
