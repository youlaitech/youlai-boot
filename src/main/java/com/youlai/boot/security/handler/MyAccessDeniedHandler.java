package com.youlai.boot.security.handler;

import com.youlai.boot.core.web.ResultCode;
import com.youlai.boot.core.web.WebResponseWriter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 无权限访问处理器
 *
 * @author Ray.Hao
 * @since 2.0.0
 */
public class MyAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) {
        WebResponseWriter.writeError(response, ResultCode.ACCESS_UNAUTHORIZED);
    }

}
