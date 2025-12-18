package com.youlai.boot.platform.ai.model.query;

import com.youlai.boot.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * AI 助手行为记录分页查询对象
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Schema(description = "AI 助手行为记录分页查询对象")
@Getter
@Setter
public class AiAssistantPageQuery extends BasePageQuery {

  @Schema(description = "关键字(原始命令/函数名称/用户名)")
  private String keywords;

  @Schema(description = "执行状态(0-待执行, 1-成功, -1-失败)")
  private Integer executeStatus;

  @Schema(description = "用户ID")
  private Long userId;

  @Schema(description = "解析状态(0-失败, 1-成功)")
  private Integer parseStatus;

  @Schema(description = "创建时间范围")
  private List<String> createTime;

  @Schema(description = "函数名称")
  private String functionName;

  @Schema(description = "AI供应商")
  private String aiProvider;

  @Schema(description = "AI模型")
  private String aiModel;
}
