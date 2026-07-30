package com.youlai.boot.auth.controller;

import com.youlai.boot.auth.model.req.WxMaBindMobileReq;
import com.youlai.boot.auth.model.req.WxMaPhoneLoginReq;
import com.youlai.boot.auth.model.resp.WxMaLoginResp;
import com.youlai.boot.auth.service.WxMaAuthService;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.common.annotation.RateLimit;
import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.framework.security.model.AuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * 微信小程序认证控制层
 *
 * @author Ray.Hao
 * @since 2.4.0
 */
@Tag(name = "01.认证中心")
@RestController
@RequestMapping("/api/v1/auth/wxma")
@RequiredArgsConstructor
@Slf4j
public class WxMaAuthController {

    private final WxMaAuthService wxMaAuthService;

    /**
     * 静默登录
     * <p>
     * 适用场景：个人小程序、无需手机号的登录场景
     * <ul>
     *   <li>已绑定手机号的用户：直接返回 token，登录成功</li>
     *   <li>未绑定手机号的用户：返回 openid，需调用绑定手机号接口</li>
     * </ul>
     */
    @Operation(summary = "[小程序]静默登录", description = "通过微信 code 登录，已绑定用户直接返回 token，未绑定用户返回 openid 需绑定手机号")
    @PostMapping("/silent-login")
    @Log(module = LogModuleEnum.LOGIN, value = ActionTypeEnum.LOGIN)
    @RateLimit
    public Result<WxMaLoginResp> silentLogin(
            @Parameter(description = "微信登录凭证（wx.login 获取）", required = true, example = "0xxx")
            @RequestParam String code
    ) {
        WxMaLoginResp result = wxMaAuthService.silentLogin(code);
        return Result.success(result);
    }

    /**
     * 手机号快捷登录
     * <p>
     * 适用场景：企业认证小程序（已开通手机号快捷登录权限）
     * <p>
     * 一步完成登录，无需绑定流程，自动创建新用户
     */
    @Operation(summary = "[小程序]手机号快捷登录", description = "同时使用微信 code 和手机号授权 code 登录，适用于企业认证小程序")
    @PostMapping("/phone-login")
    @Log(module = LogModuleEnum.LOGIN, value = ActionTypeEnum.LOGIN)
    @RateLimit
    public Result<AuthenticationToken> phoneLogin(@Valid @RequestBody WxMaPhoneLoginReq req) {
        AuthenticationToken result = wxMaAuthService.phoneLogin(req.getLoginCode(), req.getPhoneCode());
        return Result.success(result);
    }

    /**
     * 绑定手机号
     * <p>
     * 适用场景：静默登录后未绑定手机号的用户
     * <p>
     * 绑定成功后自动完成登录
     */
    @Operation(summary = "[小程序]绑定手机号", description = "为静默登录用户绑定手机号，绑定成功后自动登录")
    @PostMapping("/bind-mobile")
    @Log(module = LogModuleEnum.LOGIN, value = ActionTypeEnum.LOGIN)
    @RateLimit
    public Result<AuthenticationToken> bindMobile(@Valid @RequestBody WxMaBindMobileReq req) {
        AuthenticationToken result = wxMaAuthService.bindMobile(req.getOpenid(), req.getMobile(), req.getSmsCode());
        return Result.success(result);
    }
}
