package com.youlai.boot.auth.session.controller;

import com.youlai.boot.auth.session.service.AuthSessionService;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.framework.security.model.AuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录态管理接口
 *
 * @author Ray.Hao
 * @since 4.8.0
 */
@Tag(name = "01.认证中心")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthSessionController {

    private final AuthSessionService authSessionService;

    @Operation(summary = "退出登录")
    @DeleteMapping("/logout")
    @Log(module = LogModuleEnum.LOGIN, value = ActionTypeEnum.LOGOUT)
    public Result<Void> logout() {
        authSessionService.logout();
        return Result.success();
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh-token")
    public Result<AuthenticationToken> refreshToken(
            @Parameter(description = "刷新令牌", example = "xxx.xxx.xxx") @RequestParam String refreshToken
    ) {
        AuthenticationToken authenticationToken = authSessionService.refreshToken(refreshToken);
        return Result.success(authenticationToken);
    }

}
