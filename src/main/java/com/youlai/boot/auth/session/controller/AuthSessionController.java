package com.youlai.boot.auth.session.controller;

import com.youlai.boot.auth.session.service.AuthSessionService;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.framework.security.model.TokenPair;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /**
     * 退出登录
     *
     * <p>普通登录只退本地；统一认证登录额外返回认证中心登出地址，由前端跳转完成中央退出
     */
    @Operation(summary = "退出登录")
    @DeleteMapping("/logout")
    @Log(module = LogModuleEnum.LOGIN, value = ActionTypeEnum.LOGOUT)
    public Result<LogoutResult> logout() {
        return Result.success(new LogoutResult(authSessionService.logout()));
    }

    /**
     * 轮换刷新凭证并返回新令牌
     */
    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh-token")
    public Result<TokenPair> refreshToken(
            @RequestBody @Valid RefreshTokenRequest request
    ) {
        return Result.success(authSessionService.refreshToken(request.refreshToken()));
    }

    /**
     * 刷新令牌请求
     */
    public record RefreshTokenRequest(
            @NotBlank(message = "刷新令牌不能为空") @Size(max = 128, message = "刷新令牌长度不合法")
            @Schema(description = "刷新令牌") String refreshToken) {
    }

    /**
     * 退出登录结果
     *
     * @param logoutUrl 认证中心登出地址；为空表示只退出了本地登录
     */
    public record LogoutResult(String logoutUrl) {
    }

}
