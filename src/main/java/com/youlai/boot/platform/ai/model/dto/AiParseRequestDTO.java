package com.youlai.boot.platform.ai.model.dto;

import lombok.Data;
import java.util.Map;

/**
 * AI 解析请求 Dto
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Data
public class AiParseRequestDTO {

    /**
     * 用户输入的自然语言命令
     */
    private String command;

    /**
     * 当前页面路由（用于上下文）
     */
    private String currentRoute;

    /**
     * 当前激活的组件名称
     */
    private String currentComponent;

    /**
     * 额外上下文信息
     */
    private Map<String, Object> context;
}
