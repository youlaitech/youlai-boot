package com.youlai.boot.framework.web.log;

/**
 * 操作日志落库端口
 * <p>由 {@link com.youlai.boot.system.adapter.log.OperationLogAdapter} 实现</p>
 *
 * @author Ray.Hao
 * @since 4.7.0
 */
public interface OperationLogPort {

    /**
     * 记录操作日志
     *
     * @param operationLog 操作日志载荷
     */
    void record(OperationLog operationLog);
}
