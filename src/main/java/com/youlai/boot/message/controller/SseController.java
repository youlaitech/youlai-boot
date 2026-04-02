package com.youlai.boot.message.controller;

import com.youlai.boot.common.result.Result;
import com.youlai.boot.framework.security.model.SysUserDetails;
import com.youlai.boot.framework.security.util.SecurityUtils;
import com.youlai.boot.message.service.SseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 控制器
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
    public SseEmitter connect() {
        SysUserDetails user = SecurityUtils.getUser().orElse(null);
        if (user == null) {
            log.warn("SSE连接失败：未获取到当前用户");
            return null;
        }
        return sseService.createConnection(user.getUsername());
    }

    @Operation(summary = "获取在线用户数")
    @GetMapping("/online-count")
    public Result<Integer> getOnlineCount() {
        return Result.success(sseService.getOnlineUserCount());
    }
}
