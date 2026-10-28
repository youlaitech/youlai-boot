package com.youlai.boot.auth.wxma.controller;

import com.youlai.boot.auth.wxma.model.form.WxMaBindMobileForm;
import com.youlai.boot.auth.wxma.model.form.WxMaPhoneLoginForm;
import com.youlai.boot.auth.wxma.model.vo.WxMaLoginVO;
import com.youlai.boot.auth.wxma.service.WxMaAuthService;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.common.enums.ActionTypeEnum;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.framework.security.model.TokenPair;
import com.youlai.boot.framework.web.ratelimit.RateLimit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
public class WxMaAuthController {

    private final WxMaAuthService wxMaAuthService;

    /**
     * 校验微信登录凭证，返回会话或待绑定状态
     */
    @Operation(summary = "[小程序]静默登录", description = "通过微信 code 登录，未关联用户返回绑定手机号标记")
    @PostMapping("/silent-login")
    @Log(module = LogModuleEnum.LOGIN, value = ActionTypeEnum.LOGIN)
    @RateLimit
    public Result<WxMaLoginVO> silentLogin(
            @Parameter(description = "微信登录凭证（wx.login 获取）", required = true, example = "0xxx")
            @RequestParam String code
    ) {
        WxMaLoginVO result = wxMaAuthService.silentLogin(code);
        return Result.success(result);
    }

    /**
     * 通过微信手机号授权完成登录
     */
    @Operation(summary = "[小程序]手机号快捷登录", description = "同时使用微信 code 和手机号授权 code 登录，适用于企业认证小程序")
    @PostMapping("/phone-login")
    @Log(module = LogModuleEnum.LOGIN, value = ActionTypeEnum.LOGIN)
    @RateLimit
    public Result<TokenPair> phoneLogin(@Valid @RequestBody WxMaPhoneLoginForm req) {
        TokenPair result = wxMaAuthService.phoneLogin(req.getLoginCode(), req.getPhoneCode());
        return Result.success(result);
    }

    /**
     * 校验微信身份和手机验证码后关联本地账号
     */
    @Operation(summary = "[小程序]绑定手机号", description = "为静默登录用户绑定手机号，绑定成功后自动登录")
    @PostMapping("/bind-mobile")
    @Log(module = LogModuleEnum.LOGIN, value = ActionTypeEnum.LOGIN)
    @RateLimit
    public Result<TokenPair> bindMobile(@Valid @RequestBody WxMaBindMobileForm req) {
        TokenPair result = wxMaAuthService.bindMobile(req.getLoginCode(), req.getMobile(), req.getSmsCode());
        return Result.success(result);
    }
}
