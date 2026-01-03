package com.youlai.boot.platform.ai.model.dto;

import lombok.Data;

/**
 * AI 命令执行请求 Dto
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Data
public class AiExecuteRequestDTO {

    /**
     * 关联的解析日志ID
     */
    private String parseLogId;

    /**
     * 原始命令（用于审计）
     */
    private String originalCommand;

    /**
     * 要执行的函数调用
     */
    private AiFunctionCallDTO functionCall;

    /**
     * 确认模式：auto=自动执行, manual=需要用户确认
     */
    private String confirmMode;

    /**
     * 用户确认标志
     */
    private Boolean userConfirmed;

    /**
     * 幂等性令牌（防止重复执行）
     */
    private String idempotencyKey;

    /**
     * 当前页面路由
     */
    private String currentRoute;
}
