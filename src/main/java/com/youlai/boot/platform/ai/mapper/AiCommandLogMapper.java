package com.youlai.boot.platform.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.platform.ai.model.entity.AiCommandLog;
import com.youlai.boot.platform.ai.model.query.AiCommandPageQuery;
import com.youlai.boot.platform.ai.model.vo.AiCommandLogVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 命令记录 Mapper
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Mapper
public interface AiCommandLogMapper extends BaseMapper<AiCommandLog> {

    /**
     * 获取 AI 命令记录分页列表
     */
    IPage<AiCommandLogVO> getLogPage(Page<AiCommandLogVO> page, AiCommandPageQuery queryParams);
}

