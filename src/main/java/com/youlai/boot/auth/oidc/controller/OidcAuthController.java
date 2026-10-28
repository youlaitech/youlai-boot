package com.youlai.boot.auth.oidc.controller;

import com.youlai.boot.auth.oidc.config.ConditionalOnOidcEnabled;
import com.youlai.boot.auth.oidc.model.form.OidcTicketExchangeForm;
import com.youlai.boot.auth.oidc.service.OidcLoginService;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.framework.security.model.TokenPair;
import com.youlai.boot.framework.web.ratelimit.RateLimit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OIDC 登录票据兑换接口
 *
 * @author Ray.Hao
 * @since 4.12.0
 */
@Tag(name = "01.认证中心")
@RestController
@RequestMapping("/api/v1/auth/oidc")
@ConditionalOnOidcEnabled
@RequiredArgsConstructor
public class OidcAuthController {

    private final OidcLoginService oidcLoginService;

    /**
     * 消费一次性认证票据并创建本地会话
     */
    @Operation(summary = "[OIDC] 票据兑换登录令牌")
    @PostMapping("/tickets/exchange")
    @RateLimit
    public Result<TokenPair> exchangeTicket(@RequestBody @Valid OidcTicketExchangeForm form,
                                            HttpServletRequest request) {
        return Result.success(oidcLoginService.exchangeTicket(form.getTicket(), request));
    }
}
