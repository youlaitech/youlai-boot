package com.youlai.boot.platform.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.platform.ai.model.entity.AiAssistantRecord;
import com.youlai.boot.platform.ai.model.query.AiAssistantPageQuery;
import com.youlai.boot.platform.ai.model.vo.AiAssistantRecordVo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiAssistantRecordMapper extends BaseMapper<AiAssistantRecord> {

  /**
   * AI 助手行为记录分页列表
   *
   * @param page        分页参数
   * @param queryParams 查询参数
   * @return 分页结果
   */
  IPage<AiAssistantRecordVo> getRecordPage(Page<AiAssistantRecordVo> page, AiAssistantPageQuery queryParams);
}
