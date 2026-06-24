package com.youlai.boot.framework.security.handler;

import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.framework.web.util.ResponseWriter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 无权限访问处理器
 *
 * @author Ray.Hao
 * @since 2.0.0
 */
public class MyAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) {
        // 权限不足返回 403 Forbidden
        ResponseWriter.writeError(response, ResultCode.ACCESS_PERMISSION_EXCEPTION);
    }

}
