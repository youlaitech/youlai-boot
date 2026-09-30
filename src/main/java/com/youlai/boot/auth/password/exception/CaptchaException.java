package com.youlai.boot.auth.password.exception;

import com.youlai.boot.common.result.ResultCode;
import lombok.Getter;

/**
 * 图形验证码校验失败时抛出的异常。
 *
 * @author Ray.Hao
 * @since 3.1.0
 */
@Getter
public class CaptchaException extends RuntimeException {

    private final ResultCode resultCode;

    public CaptchaException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.resultCode = resultCode;
    }

}
