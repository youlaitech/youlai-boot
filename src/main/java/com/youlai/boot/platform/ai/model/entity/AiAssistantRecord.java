package com.youlai.boot.platform.ai.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * AI 助手行为记录实体
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_assistant_record")
public class AiAssistantRecord extends BaseEntity {

  /** 用户ID */
  private Long userId;

  /** 用户名 */
  private String username;

  /** 原始命令 */
  private String originalCommand;

  // ==================== 解析相关字段 ====================

  /** AI 供应商（qwen/openai/deepseek等） */
  private String aiProvider;

  /** AI 模型（qwen-plus/qwen-max/gpt-4-turbo等） */
  private String aiModel;

  /** 解析状态（0-失败, 1-成功） */
  private Integer parseStatus;

  /** 解析出的函数调用列表（JSON） */
  private String functionCalls;

  /** AI 的理解说明 */
  private String explanation;

  /** 置信度（0.00-1.00） */
  private BigDecimal confidence;

  /** 解析错误信息 */
  private String parseErrorMessage;

  /** 输入 Token 数量 */
  private Integer inputTokens;

  /** 输出 Token 数量 */
  private Integer outputTokens;

  /** 解析耗时（毫秒） */
  private Integer parseDurationMs;

  // ==================== 执行相关字段 ====================

  /** 执行的函数名称 */
  private String functionName;

  /** 函数参数（JSON） */
  private String functionArguments;

  /** 执行状态（0-待执行, 1-成功, -1-失败） */
  private Integer executeStatus;

  /** 执行错误信息 */
  private String executeErrorMessage;

  // ==================== 通用字段 ====================

  /** IP 地址 */
  private String ipAddress;
}
