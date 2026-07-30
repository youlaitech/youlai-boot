package com.youlai.boot.auth.controller;

import com.youlai.boot.auth.service.AuthService;
import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.common.annotation.RateLimit;
import com.youlai.boot.framework.security.model.AuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短信验证码认证接口
 *
 * @author Ray.Hao
 * @since 4.5.0
 */
@Tag(name = "01.认证中心")
@RestController
@RequestMapping("/api/v1/auth/sms")
@RequiredArgsConstructor
@Slf4j
public class SmsAuthController {

    private final AuthService authService;

    @Operation(summary = "[短信]发送登录短信验证码")
    @PostMapping("/code")
    @RateLimit(limit = 1, window = 60)
    public Result<Void> sendCode(
            @Parameter(description = "手机号", example = "18888888888") @RequestParam String mobile
    ) {
        authService.sendSmsCode(mobile);
        return Result.success();
    }

    @Operation(summary = "[短信]短信验证码登录")
    @PostMapping("/login")
    @Log(module = LogModuleEnum.LOGIN, value = ActionTypeEnum.LOGIN)
    public Result<AuthenticationToken> login(
            @Parameter(description = "手机号", example = "18888888888") @RequestParam String mobile,
            @Parameter(description = "验证码", example = "123456") @RequestParam String code
    ) {
        AuthenticationToken loginResult = authService.loginBySms(mobile, code);
        return Result.success(loginResult);
    }

}
