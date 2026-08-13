package com.youlai.boot.framework.web.log;

/**
 * 操作日志落库端口
 * <p>由各业务模块实现，framework 层通过此接口写入操作日志，不依赖具体落库实现。
 * 如 system 模块的 {@code OperationLogAdapter} 落库 sys_log 表。</p>
 *
 * @see com.youlai.boot.system.adapter.log.OperationLogAdapter
 * @author Ray.Hao
 * @since 0.0.1
 */
public interface OperationLogPort {

    /**
     * 记录操作日志
     *
     * @param operationLog 操作日志载荷
     */
    void record(OperationLog operationLog);
}
