package com.youlai.boot.platform.ai.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.platform.ai.mapper.AiCommandRecordMapper;
import com.youlai.boot.platform.ai.model.entity.AiCommandRecord;
import com.youlai.boot.platform.ai.model.query.AiCommandPageQuery;
import com.youlai.boot.platform.ai.model.vo.AiCommandRecordVO;
import com.youlai.boot.platform.ai.service.AiCommandRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI 命令记录服务实现类
 *
 * @author Ray.Hao
 * @since 3.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AiCommandRecordServiceImpl extends ServiceImpl<AiCommandRecordMapper, AiCommandRecord>
        implements AiCommandRecordService {

    @Override
    public IPage<AiCommandRecordVO> getRecordPage(AiCommandPageQuery queryParams) {
        Page<AiCommandRecordVO> page = new Page<>(queryParams.getPageNum(), queryParams.getPageSize());
        return this.baseMapper.getRecordPage(page, queryParams);
    }

    @Override
    public void rollbackCommand(String logId) {
        AiCommandRecord commandRecord = this.getById(logId);
        if (commandRecord == null) {
            throw new RuntimeException("命令记录不存在");
        }

        if (commandRecord.getExecuteStatus() == null || commandRecord.getExecuteStatus() != 1) {
            throw new RuntimeException("只能撤销成功执行的命令");
        }

        // TODO: 实现具体的回滚逻辑
        log.info("撤销命令执行: logId={}, function={}", logId, commandRecord.getFunctionName());
        throw new UnsupportedOperationException("回滚功能尚未实现");
    }
}


