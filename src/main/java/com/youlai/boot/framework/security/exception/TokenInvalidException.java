package com.youlai.boot.framework.security.exception;

import com.youlai.boot.common.result.ResultCode;
import lombok.Getter;

/**
 * Token 无效异常（access_token 或 refresh_token 过期/无效）
 *
 * @author Ray.Hao
 * @since 4.3.1
 */
@Getter
public class TokenInvalidException extends RuntimeException {

    private final ResultCode resultCode;

    public TokenInvalidException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.resultCode = resultCode;
    }
}
