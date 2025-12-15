package com.youlai.boot.security.filter;

import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.constant.SecurityConstants;
import com.youlai.boot.core.web.ResultCode;
import com.youlai.boot.core.web.WebResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 图形验证码校验过滤器
 */
public class CaptchaValidationFilter extends OncePerRequestFilter {

    private static final RequestMatcher LOGIN_PATH_REQUEST_MATCHER = PathPatternRequestMatcher.withDefaults()
            .matcher(HttpMethod.POST, SecurityConstants.LOGIN_PATH);

    public static final String CAPTCHA_CODE_PARAM_NAME = "captchaCode";
    public static final String CAPTCHA_ID_PARAM_NAME = "captchaId";

    private final RedisTemplate<String, Object> redisTemplate;
    private final CodeGenerator codeGenerator;

    public CaptchaValidationFilter(RedisTemplate<String, Object> redisTemplate, CodeGenerator codeGenerator) {
        this.redisTemplate = redisTemplate;
        this.codeGenerator = codeGenerator;
    }

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 非登录接口直接放行
        if (!LOGIN_PATH_REQUEST_MATCHER.matches(request)) {
            chain.doFilter(request, response);
            return;
        }

        // 仅支持 JSON 登录
        String contentType = request.getContentType();
        if (contentType == null || !contentType.contains(MediaType.APPLICATION_JSON_VALUE)) {
            WebResponseWriter.writeError(response, ResultCode.USER_VERIFICATION_CODE_ERROR);
            return;
        }

        // 包装请求，确保下游还能读取 body
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);

        byte[] bodyBytes = StreamUtils.copyToByteArray(requestWrapper.getInputStream());
        String body = new String(bodyBytes, StandardCharsets.UTF_8);
        String captchaCode = null;
        String captchaId = null;

        if (StrUtil.isNotBlank(body)) {
            JSONObject jsonObject = JSONUtil.parseObj(body);
            captchaCode = jsonObject.getStr(CAPTCHA_CODE_PARAM_NAME);
            captchaId = jsonObject.getStr(CAPTCHA_ID_PARAM_NAME);
        }

        if (StrUtil.isBlank(captchaCode) || StrUtil.isBlank(captchaId)) {
            WebResponseWriter.writeError(response, ResultCode.USER_VERIFICATION_CODE_ERROR);
            return;
        }

        String cacheVerifyCode = (String) redisTemplate.opsForValue().get(
                StrUtil.format(RedisConstants.Captcha.IMAGE_CODE, captchaId)
        );
        if (cacheVerifyCode == null) {
            WebResponseWriter.writeError(response, ResultCode.USER_VERIFICATION_CODE_EXPIRED);
            return;
        }

        if (codeGenerator.verify(cacheVerifyCode, captchaCode)) {
            HttpServletRequest repeatableRequest = new RepeatableReadRequestWrapper(requestWrapper, bodyBytes);
            chain.doFilter(repeatableRequest, response);
        } else {
            WebResponseWriter.writeError(response, ResultCode.USER_VERIFICATION_CODE_ERROR);
        }
    }

    /**
     * Simple wrapper to allow repeated reads of the request body after we've parsed it here.
     */
    private static class RepeatableReadRequestWrapper extends HttpServletRequestWrapper {

        private final byte[] cachedBody;

        RepeatableReadRequestWrapper(HttpServletRequest request, byte[] cachedBody) {
            super(request);
            this.cachedBody = cachedBody != null ? cachedBody : new byte[0];
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return bais.read();
                }

                @Override
                public boolean isFinished() {
                    return bais.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(jakarta.servlet.ReadListener readListener) {
                    // no-op
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() {
            return cachedBody.length;
        }

        @Override
        public long getContentLengthLong() {
            return cachedBody.length;
        }
    }
}


