package com.youlai.boot.system.adapter.log;

import com.youlai.boot.framework.web.log.OperationLog;
import com.youlai.boot.framework.web.log.OperationLogPort;
import com.youlai.boot.system.converter.LogConverter;
import com.youlai.boot.system.model.entity.SysLog;
import com.youlai.boot.system.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 操作日志适配器
 * <p>实现 framework 层的 {@link OperationLogPort}，将切面采集的操作日志落库 sys_log 表</p>
 *
 * @author Ray.Hao
 * @since 0.0.1
 * @see OperationLogPort
 */
@Component
@RequiredArgsConstructor
public class OperationLogAdapter implements OperationLogPort {

    private final LogService logService;
    private final LogConverter logConverter;

    /**
     * 记录操作日志
     * <p>通过 {@link LogConverter} 将操作日志载荷转换为 {@link SysLog} 后落库</p>
     *
     * @param operationLog 操作日志载荷
     */
    @Override
    public void record(OperationLog operationLog) {
        logService.save(logConverter.toEntity(operationLog));
    }
}
