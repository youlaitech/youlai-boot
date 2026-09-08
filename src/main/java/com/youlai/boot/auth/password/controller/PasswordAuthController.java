package com.youlai.boot.auth.password.controller;

import com.youlai.boot.auth.password.model.form.LoginForm;
import com.youlai.boot.auth.password.model.vo.CaptchaVO;
import com.youlai.boot.auth.password.service.PasswordAuthService;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.framework.security.model.AuthenticationToken;
import com.youlai.boot.framework.web.ratelimit.RateLimit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账号密码认证接口
 *
 * @author Ray.Hao
 * @since 4.8.0
 */
@Tag(name = "01.认证中心")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class PasswordAuthController {

    private final PasswordAuthService passwordAuthService;

    @Operation(summary = "获取验证码")
    @GetMapping("/captcha")
    public Result<CaptchaVO> getCaptcha() {
        CaptchaVO captcha = passwordAuthService.getCaptcha();
        return Result.success(captcha);
    }

    @Operation(summary = "账号密码登录")
    @PostMapping("/login")
    @Log(module = LogModuleEnum.LOGIN, value = ActionTypeEnum.LOGIN)
    @RateLimit
    public Result<AuthenticationToken> login(@RequestBody @Valid LoginForm request) {
        AuthenticationToken authenticationToken = passwordAuthService.login(request.getUsername(), request.getPassword());
        return Result.success(authenticationToken);
    }

}
