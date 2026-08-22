package com.youlai.boot.framework.web.log;

import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;

import java.time.LocalDateTime;

/**
 * 操作日志载荷
 *
 * @author Ray.Hao
 * @since 4.7.0
 */
public record OperationLog(
        /** 模块 */
        LogModuleEnum module,
        /** 操作类型 */
        ActionTypeEnum actionType,
        /** 操作标题 */
        String title,
        /** 自定义日志内容 */
        String content,
        /** 操作人 ID */
        Long operatorId,
        /** 操作人名称 */
        String operatorName,
        /** 请求路径 */
        String requestUri,
        /** 请求方式 */
        String requestMethod,
        /** IP 地址 */
        String ip,
        /** 省份 */
        String province,
        /** 城市 */
        String city,
        /** 设备 */
        String device,
        /** 操作系统 */
        String os,
        /** 浏览器 */
        String browser,
        /** 状态：0 失败 1 成功 */
        Integer status,
        /** 错误信息 */
        String errorMsg,
        /** 执行时间（毫秒） */
        Integer executionTime,
        /** 操作时间 */
        LocalDateTime createTime
) {
}
