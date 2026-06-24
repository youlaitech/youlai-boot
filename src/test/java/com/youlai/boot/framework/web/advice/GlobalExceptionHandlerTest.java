package com.youlai.boot.framework.web.advice;

import com.youlai.boot.common.result.Result;
import com.youlai.boot.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void should_not_expose_unknown_exception_message() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        Result<Void> result = handler.handleException(new IllegalStateException("database password leaked"));

        assertThat(result.getCode()).isEqualTo(ResultCode.SYSTEM_ERROR.getCode());
        assertThat(result.getMsg()).isEqualTo("系统异常，请稍后重试");
    }
}
