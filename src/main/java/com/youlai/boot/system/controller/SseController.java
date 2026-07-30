package com.youlai.boot.system.controller;

import com.youlai.boot.support.sse.SseService;
import com.youlai.boot.framework.security.model.SecurityUserDetails;
import com.youlai.boot.framework.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * SSE 连接控制器
 *
 * @author Ray.Hao
 * @since 3.1.0
 */
@Tag(name = "14. SSE连接")
@Slf4j
@RestController
@RequestMapping("/api/v1/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;

    @Operation(summary = "建立SSE连接")
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(HttpServletResponse response) throws IOException {
        SecurityUserDetails user = SecurityUtils.getUser().orElse(null);
        if (user == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.flushBuffer();
            return null;
        }
        return sseService.createConnection(user.getUsername());
    }
}
