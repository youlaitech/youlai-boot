package com.youlai.boot.platform.ai.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 命令执行响应 Dto
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiExecuteResponseDto {

    /**
     * 是否执行成功
     */
    private Boolean success;

    /**
     * 执行结果数据
     */
    private Object data;

    /**
     * 执行结果说明
     */
    private String message;

    /**
     * 影响的记录数
     */
    private Integer affectedRows;

    /**
     * 错误信息
     */
    private String error;

    /**
     * 记录ID（用于追踪）
     */
    private Long recordId;

    /**
     * 需要用户确认
     */
    private Boolean requiresConfirmation;

    /**
     * 确认提示信息
     */
    private String confirmationPrompt;
}
