package com.youlai.boot.system.controller;

import com.youlai.boot.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统健康检查接口，返回 ok。
 * 不标注 @RateLimit，仅受 IP 全局限流层约束，用作该层的验证端点。
 */
@Tag(name = "00.系统健康检查")
@RestController
@RequestMapping("/api/v1/health")
@Slf4j
public class HealthController {

    @Operation(summary = "健康检查", description = "返回 ok，供限流测试与探针使用")
    @GetMapping
    public Result<String> health() {
        return Result.success("ok");
    }
}
