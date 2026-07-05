package com.youlai.boot.auth.security.handler;

import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.framework.web.util.ResponseWriter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 统一处理 Spring Security 认证失败响应。
 * <p>
 * 归使用方，因为 JSON 响应格式（Result 结构、错误码）因项目而异。
 *
 * @author Ray.Hao
 * @since 2.0.0
 */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        if (authException instanceof BadCredentialsException) {
            ResponseWriter.writeError(response, ResultCode.USER_PASSWORD_ERROR);
        } else if (authException instanceof InsufficientAuthenticationException) {
            ResponseWriter.writeError(response, ResultCode.ACCESS_TOKEN_INVALID);
        } else {
            ResponseWriter.writeError(response, ResultCode.USER_LOGIN_EXCEPTION, authException.getMessage());
        }
    }
}
