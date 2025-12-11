package com.youlai.boot.platform.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.platform.ai.model.entity.AiCommandRecord;
import com.youlai.boot.platform.ai.model.query.AiCommandPageQuery;
import com.youlai.boot.platform.ai.model.vo.AiCommandRecordVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 命令记录 Mapper
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Mapper
public interface AiCommandRecordMapper extends BaseMapper<AiCommandRecord> {

    /**
     * 获取 AI 命令记录分页列表
     */
    IPage<AiCommandRecordVO> getRecordPage(Page<AiCommandRecordVO> page, AiCommandPageQuery queryParams);
}


