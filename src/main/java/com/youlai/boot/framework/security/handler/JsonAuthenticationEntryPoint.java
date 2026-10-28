package com.youlai.boot.framework.security.handler;

import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.framework.web.util.ResponseWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 统一处理 Spring Security 认证失败响应
 *
 * @author Ray.Hao
 * @since 2.0.0
 */
@Slf4j
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        if (authException instanceof AuthenticationServiceException) {
            log.error("认证服务不可用", authException);
            ResponseWriter.writeError(response, ResultCode.AUTH_SERVICE_UNAVAILABLE);
        } else {
            ResponseWriter.writeError(response, ResultCode.ACCESS_TOKEN_INVALID);
        }
    }
}
