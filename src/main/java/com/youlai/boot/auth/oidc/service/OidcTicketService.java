package com.youlai.boot.auth.oidc.service;

import com.youlai.boot.framework.security.token.SecureTokenGenerator;
import com.youlai.boot.framework.security.session.LoginContext;
import com.youlai.boot.auth.oidc.config.ConditionalOnOidcEnabled;
import com.youlai.boot.auth.oidc.model.OidcTicketContext;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.common.exception.TokenInvalidException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;

/**
 * 签发并消费绑定浏览器的一次性登录票据
 *
 * @author Ray.Hao
 * @since 4.12.0
 */
@Service
@ConditionalOnOidcEnabled
@RequiredArgsConstructor
public class OidcTicketService {

    public static final String BROWSER_BINDING_COOKIE = "YOULAI_OIDC_BIND";
    private static final Duration TICKET_TTL = Duration.ofSeconds(45);

    // 校验浏览器绑定后删除，票据只能兑换一次
    private static final DefaultRedisScript<String> CONSUME = new DefaultRedisScript<>("""
            local raw = redis.call('GET', KEYS[1])
            if not raw then return nil end
            if cjson.decode(raw).browserBinding ~= ARGV[1] then return nil end
            redis.call('DEL', KEYS[1])
            return raw
            """, String.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final JsonMapper jsonMapper;

    /**
     * 签发票据，并用 HttpOnly Cookie 绑定当前浏览器
     */
    public String issue(Long userId, LoginContext loginContext,
                        HttpServletRequest request, HttpServletResponse response) {
        String binding = SecureTokenGenerator.generate();
        var context = new OidcTicketContext(userId, loginContext, SecureTokenGenerator.digest(binding));
        String ticket = SecureTokenGenerator.generate();
        stringRedisTemplate.opsForValue().set(getTicketKey(ticket), jsonMapper.writeValueAsString(context), TICKET_TTL);

        ResponseCookie cookie = ResponseCookie.from(BROWSER_BINDING_COOKIE, binding)
                .httpOnly(true)
                .secure(request.isSecure())
                .path("/")
                .maxAge(TICKET_TTL)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ticket;
    }

    /**
     * 校验浏览器绑定后原子消费票据
     */
    public OidcTicketContext consume(String ticket, HttpServletRequest request) {
        String binding = getBrowserBinding(request);
        if (StringUtils.isBlank(ticket) || StringUtils.isBlank(binding)) {
            throw new TokenInvalidException(ResultCode.OIDC_TICKET_INVALID);
        }

        String json = stringRedisTemplate.execute(CONSUME, List.of(getTicketKey(ticket)),
                SecureTokenGenerator.digest(binding));
        if (StringUtils.isBlank(json)) {
            throw new TokenInvalidException(ResultCode.OIDC_TICKET_INVALID);
        }

        return jsonMapper.readValue(json, OidcTicketContext.class);
    }

    /**
     * 读取用于票据校验的浏览器绑定 Cookie
     */
    private String getBrowserBinding(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (BROWSER_BINDING_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    /**
     * 通过票据摘要构造存储键
     */
    private String getTicketKey(String ticket) {
        return RedisConstants.Auth.OIDC_TICKET.replace("{}", SecureTokenGenerator.digest(ticket));
    }
}
