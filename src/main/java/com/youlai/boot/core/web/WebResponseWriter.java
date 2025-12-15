package com.youlai.boot.core.web;

import cn.hutool.extra.servlet.JakartaServletUtil;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

/**
 * Web响应写入器
 * <p>
 * 用于在过滤器、Security处理器等无法使用 @RestControllerAdvice 的场景中统一写入HTTP响应。
 * 支持写入成功响应和错误响应。
 * 此类为工具类，所有方法均为静态方法，禁止实例化。
 *
 * @author Ray.Hao
 * @since 2.0.0
 */
@Slf4j
public final class WebResponseWriter {

    /**
     * 私有构造函数，防止实例化
     */
    private WebResponseWriter() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 写入成功响应
     *
     * @param response HttpServletResponse
     * @param data     响应数据（可选）
     */
    public static void writeSuccess(HttpServletResponse response, Object data) {
        writeResult(response, Result.success(data), HttpStatus.OK.value());
    }

    /**
     * 写入成功响应（无数据）
     *
     * @param response HttpServletResponse
     */
    public static void writeSuccess(HttpServletResponse response) {
        writeSuccess(response, null);
    }

    /**
     * 写入错误响应
     *
     * @param response   HttpServletResponse
     * @param resultCode 响应结果码
     */
    public static void writeError(HttpServletResponse response, ResultCode resultCode) {
        writeError(response, resultCode, null);
    }

    /**
     * 写入错误响应（带自定义消息）
     *
     * @param response   HttpServletResponse
     * @param resultCode 响应结果码
     * @param message    自定义消息（可选，为 null 时使用 resultCode 的默认消息）
     */
    public static void writeError(HttpServletResponse response, ResultCode resultCode, String message) {
        Result<?> result = message == null
                ? Result.failed(resultCode)
                : Result.failed(resultCode, message);
        
        int httpStatus = mapHttpStatus(resultCode);
        writeResult(response, result, httpStatus);
    }

    /**
     * 写入响应结果（通用方法）
     *
     * @param response   HttpServletResponse
     * @param result     响应结果对象
     * @param httpStatus HTTP状态码
     */
    private static void writeResult(HttpServletResponse response, Result<?> result, int httpStatus) {
        try {
            // 设置HTTP状态码
            response.setStatus(httpStatus);
            
            // 设置响应编码和内容类型
            response.setCharacterEncoding(StandardCharsets.UTF_8.toString());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            
            // 写入响应
            JakartaServletUtil.write(response,
                    JSONUtil.toJsonStr(result),
                    MediaType.APPLICATION_JSON_VALUE
            );

        } catch (Exception e) {
            log.error("写入响应时发生未知异常: httpStatus={}, result={}", httpStatus, result, e);
        }
    }

    /**
     * 根据业务结果码映射HTTP状态码
     *
     * @param resultCode 业务结果码
     * @return HTTP状态码
     */
    private static int mapHttpStatus(ResultCode resultCode) {
        return switch (resultCode) {
            case ACCESS_UNAUTHORIZED,
                    ACCESS_TOKEN_INVALID,
                    REFRESH_TOKEN_INVALID -> HttpStatus.UNAUTHORIZED.value();
            default -> HttpStatus.BAD_REQUEST.value();
        };
    }
}




