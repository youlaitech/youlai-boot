package com.youlai.boot.auth.security.handler;

import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.framework.web.util.ResponseWriter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 无权限访问处理器。
 * <p>
 * 归使用方，因为 JSON 响应格式因项目而异。
 *
 * @author Ray.Hao
 * @since 2.0.0
 */
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) {
        ResponseWriter.writeError(response, ResultCode.ACCESS_PERMISSION_EXCEPTION);
    }
}
