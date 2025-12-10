package com.youlai.boot.platform.ai.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.youlai.boot.platform.ai.model.entity.AiCommandLog;
import com.youlai.boot.platform.ai.model.query.AiCommandPageQuery;
import com.youlai.boot.platform.ai.model.vo.AiCommandLogVO;

/**
 * AI 命令记录服务接口
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
public interface AiCommandLogService extends IService<AiCommandLog> {

    /**
     * 获取命令记录分页列表
     *
     * @param queryParams 查询参数
     * @return 命令记录分页列表
     */
    IPage<AiCommandLogVO> getLogPage(AiCommandPageQuery queryParams);

    /**
     * 撤销命令执行
     *
     * @param logId 记录ID
     */
    void rollbackCommand(String logId);
}

