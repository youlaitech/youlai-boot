package com.youlai.boot.framework.web.exception;

import com.youlai.boot.common.result.ResultCode;
import lombok.Getter;

/**
 * 接口限流异常：请求超出限流阈值时抛出，由全局异常处理器统一映射为 HTTP 429。
 *
 * @author Ray.Hao
 * @since 4.4.0
 */
@Getter
public class RateLimitException extends RuntimeException {

    private final ResultCode resultCode;

    public RateLimitException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.resultCode = resultCode;
    }

    public RateLimitException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
}
