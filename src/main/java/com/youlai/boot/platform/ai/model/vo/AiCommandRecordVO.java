package com.youlai.boot.platform.ai.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI命令记录VO
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Data
@Schema(description = "AI命令记录VO")
public class AiCommandRecordVO implements Serializable {

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "原始命令")
    private String originalCommand;

    // ==================== 解析相关字段 ====================

    @Schema(description = "AI供应商")
    private String aiProvider;

    @Schema(description = "AI模型")
    private String aiModel;

    @Schema(description = "解析状态(0-失败, 1-成功)")
    private Integer parseStatus;

    @Schema(description = "解析出的函数调用列表(JSON)")
    private String functionCalls;

    @Schema(description = "AI的理解说明")
    private String explanation;

    @Schema(description = "置信度")
    private BigDecimal confidence;

    @Schema(description = "解析错误信息")
    private String parseErrorMessage;

    @Schema(description = "输入Token数量")
    private Integer inputTokens;

    @Schema(description = "输出Token数量")
    private Integer outputTokens;

    @Schema(description = "解析耗时(毫秒)")
    private Integer parseDurationMs;

    // ==================== 执行相关字段 ====================

    @Schema(description = "执行的函数名称")
    private String functionName;

    @Schema(description = "函数参数(JSON)")
    private String functionArguments;

    @Schema(description = "执行状态(0-待执行, 1-成功, -1-失败)")
    private Integer executeStatus;

    @Schema(description = "执行错误信息")
    private String executeErrorMessage;

    // ==================== 通用字段 ====================

    @Schema(description = "IP地址")
    private String ipAddress;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}

