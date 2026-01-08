package com.youlai.boot.platform.ai.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.youlai.boot.platform.ai.model.dto.AiExecuteRequestDTO;
import com.youlai.boot.platform.ai.model.dto.AiParseRequestDTO;
import com.youlai.boot.platform.ai.model.dto.AiParseResponseDTO;
import com.youlai.boot.platform.ai.model.entity.AiAssistantRecord;
import com.youlai.boot.platform.ai.model.query.AiAssistantQuery;
import com.youlai.boot.platform.ai.model.vo.AiAssistantRecordVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * AI 助手行为记录服务接口
 *
 * 负责 AI 助手指令的解析/执行审计记录的分页查询、删除与回滚。
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
public interface AiAssistantRecordService extends IService<AiAssistantRecord> {

  /**
   * 解析自然语言命令。
   *
   * @param request     解析请求参数
   * @param httpRequest HTTP 请求（用于获取 IP 等上下文）
   * @return 解析结果（包含 functionCalls 等信息）
   */
  AiParseResponseDTO parseCommand(AiParseRequestDTO request, HttpServletRequest httpRequest);

  /**
   * 执行已解析的命令。
   *
   * @param request     执行请求参数
   * @param httpRequest HTTP 请求（用于获取 IP 等上下文）
   * @return 执行结果
   * @throws Exception 执行异常
   */
  Object executeCommand(AiExecuteRequestDTO request, HttpServletRequest httpRequest) throws Exception;

  /**
   * 获取 AI 助手行为记录分页列表
   *
   * @param queryParams 查询参数
   * @return 分页列表
   */
  IPage<AiAssistantRecordVO> getRecordPage(AiAssistantQuery queryParams);

  /**
   * 删除 AI 助手行为记录。
   *
   * @param ids 记录ID列表
   * @return 是否删除成功
   */
  boolean deleteRecords(List<Long> ids);

  /**
   * 撤销命令执行
   *
   * @param logId 记录ID
   */
  void rollbackCommand(String logId);
}
