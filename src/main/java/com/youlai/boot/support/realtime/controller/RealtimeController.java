package com.youlai.boot.support.realtime.controller;

import com.youlai.boot.common.constant.SecurityConstants;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.common.result.ResultCode;
import com.youlai.boot.framework.web.ratelimit.RateLimit;
import com.youlai.boot.support.realtime.RealtimeSessionManager;
import com.youlai.boot.support.realtime.config.RealtimeProperties;
import com.youlai.boot.support.realtime.model.CreateRealtimeTicketRequest;
import com.youlai.boot.support.realtime.model.PresenceSnapshot;
import com.youlai.boot.support.realtime.model.RealtimeTicketResponse;
import com.youlai.boot.support.realtime.security.RealtimeTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供实时连接票据与在线状态查询
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@RestController
@RequestMapping("/api/v1/realtime")
@RequiredArgsConstructor
public class RealtimeController {
    private final RealtimeTicketService tickets;
    private final RealtimeSessionManager sessions;
    private final RealtimeProperties properties;

    /**
     * 验证访问凭证并签发一次性握手票据
     */
    @PostMapping("/tickets")
    @RateLimit(limit = 60, window = 60)
    public ResponseEntity<Result<RealtimeTicketResponse>> createTicket(
            @Valid @RequestBody CreateRealtimeTicketRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        if (!properties.isEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).cacheControl(CacheControl.noStore())
                    .body(Result.failed(ResultCode.REALTIME_DISABLED));
        }
        if (sessions.isStopping()) {
            return unavailable();
        }
        try {
            if (authorization == null || !authorization.startsWith(SecurityConstants.BEARER_TOKEN_PREFIX)) {
                throw new BadCredentialsException("Bearer credential required");
            }
            String accessToken = authorization.substring(SecurityConstants.BEARER_TOKEN_PREFIX.length()).trim();
            var ticket = tickets.create(accessToken, request);
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Result.success(ticket));
        } catch (AccessDeniedException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).cacheControl(CacheControl.noStore())
                    .body(Result.failed(ResultCode.ACCESS_UNAUTHORIZED));
        } catch (AuthenticationException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).cacheControl(CacheControl.noStore())
                    .body(Result.failed(ResultCode.ACCESS_TOKEN_INVALID));
        } catch (DataAccessException exception) {
            return unavailable();
        }
    }

    /**
     * 返回实时服务暂不可用响应
     */
    private ResponseEntity<Result<RealtimeTicketResponse>> unavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).cacheControl(CacheControl.noStore())
                .body(Result.failed("实时通信暂不可用"));
    }

    /**
     * 查询当前节点在线状态
     */
    @GetMapping("/presence")
    @PreAuthorize("@ss.hasPerm('sys:online:view')")
    public ResponseEntity<Result<PresenceSnapshot>> getPresence() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Result.success(sessions.presence()));
    }
}
