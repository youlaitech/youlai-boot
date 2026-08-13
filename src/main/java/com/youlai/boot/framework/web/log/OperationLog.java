package com.youlai.boot.framework.web.log;

import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;

import java.time.LocalDateTime;

/**
 * 操作日志通用载荷
 * <p>由操作日志切面采集，经 {@link OperationLogPort} 交给各业务模块落库</p>
 *
 * @author Ray.Hao
 * @since 0.0.1
 */
public record OperationLog(
        LogModuleEnum module,
        ActionTypeEnum actionType,
        String title,
        String content,
        Long operatorId,
        String operatorName,
        String requestUri,
        String requestMethod,
        String ip,
        String province,
        String city,
        String device,
        String os,
        String browser,
        Integer status,
        String errorMsg,
        Integer executionTime,
        LocalDateTime createTime
) {
}
