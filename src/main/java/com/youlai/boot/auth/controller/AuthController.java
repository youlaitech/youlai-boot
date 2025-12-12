package com.youlai.boot.auth.controller;

import com.youlai.boot.auth.model.vo.CaptchaVO;
import com.youlai.boot.auth.model.vo.ChooseTenantVO;
import com.youlai.boot.auth.model.dto.LoginRequest;
import com.youlai.boot.auth.model.dto.WxMiniAppPhoneLoginDTO;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.config.property.TenantProperties;
import com.youlai.boot.core.web.Result;
import com.youlai.boot.auth.service.AuthService;
import com.youlai.boot.auth.model.dto.WxMiniAppCodeLoginDTO;
import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.core.web.ResultCode;
import com.youlai.boot.security.model.AuthenticationToken;
import com.youlai.boot.system.model.entity.User;
import com.youlai.boot.system.model.vo.TenantVO;
import com.youlai.boot.system.service.TenantService;
import com.youlai.boot.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;


/**
 * 认证控制层
 *
 * @author Ray.Hao
 * @since 2022/10/16
 */
@Tag(name = "01.认证中心")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final TenantService tenantService;
    private final TenantProperties tenantProperties;

    @Operation(summary = "获取验证码")
    @GetMapping("/captcha")
    public Result<CaptchaVO> getCaptcha() {
        CaptchaVO captcha = authService.getCaptcha();
        return Result.success(captcha);
    }

    @Operation(summary = "账号密码登录")
    @PostMapping("/login")
    @Log(value = "登录", module = LogModuleEnum.LOGIN)
    public Result<?> login(@RequestBody @Valid LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();
        Long tenantId = request.getTenantId();

        // 如果未启用多租户，直接登录
        if (tenantProperties == null || !Boolean.TRUE.equals(tenantProperties.getEnabled())) {
            AuthenticationToken authenticationToken = authService.login(username, password, null);
            return Result.success(authenticationToken);
        }

        // 多租户模式：如果指定了租户ID，直接验证该租户下的密码
        if (tenantId != null) {
            AuthenticationToken authenticationToken = authService.login(username, password, tenantId);
            return Result.success(authenticationToken);
        }

        // 多租户模式：未指定租户ID，查询该用户名在所有租户下的账户
        List<User> users = userService.findUserAcrossAllTenants(username);

        if (users.isEmpty()) {
            return Result.failed("用户不存在");
        }

        // 过滤出正常状态的用户
        List<User> activeUsers = users.stream()
                .filter(user -> user.getStatus() != null && user.getStatus() == 1)
                .toList();

        if (activeUsers.isEmpty()) {
            return Result.failed("用户已被禁用");
        }

        // 如果只有1个租户，尝试验证该租户下的密码（兼容性）
        if (activeUsers.size() == 1) {
            User user = activeUsers.get(0);
            // 登录（Spring Security 会验证密码）
            AuthenticationToken authenticationToken = authService.login(username, password, user.getTenantId());
            return Result.success(authenticationToken);
        }

        // 如果多个租户，返回 choose_tenant 响应（含 tenants 列表）
        // 注意：此时不验证密码，直接返回租户列表让用户选择
        List<TenantVO> tenants = activeUsers.stream()
                .map(user -> tenantService.getTenantById(user.getTenantId()))
                .filter(tenant -> tenant != null && (tenant.getStatus() == null || tenant.getStatus() == 1))
                .distinct() // 去重（理论上不会有重复，但保险起见）
                .collect(Collectors.toList());

        if (tenants.isEmpty()) {
            return Result.failed("用户所属的租户均不可用");
        }

        // 返回 choose_tenant 响应
        ChooseTenantVO chooseTenantVO = new ChooseTenantVO(tenants);
        return Result.failed(ResultCode.CHOOSE_TENANT, chooseTenantVO);
    }

    @Operation(summary = "短信验证码登录")
    @PostMapping("/login/sms")
    @Log(value = "短信验证码登录", module = LogModuleEnum.LOGIN)
    public Result<AuthenticationToken> loginBySms(
            @Parameter(description = "手机号", example = "18812345678") @RequestParam String mobile,
            @Parameter(description = "验证码", example = "1234") @RequestParam String code
    ) {
        AuthenticationToken loginResult = authService.loginBySms(mobile, code);
        return Result.success(loginResult);
    }

    @Operation(summary = "发送登录短信验证码")
    @PostMapping("/sms/code")
    public Result<Void> sendLoginVerifyCode(
            @Parameter(description = "手机号", example = "18812345678") @RequestParam String mobile
    ) {
        authService.sendSmsLoginCode(mobile);
        return Result.success();
    }

    @Operation(summary = "微信授权登录(Web)")
    @PostMapping("/login/wechat")
    @Log(value = "微信登录", module = LogModuleEnum.LOGIN)
    public Result<AuthenticationToken> loginByWechat(
            @Parameter(description = "微信授权码", example = "code") @RequestParam String code
    ) {
        AuthenticationToken loginResult = authService.loginByWechat(code);
        return Result.success(loginResult);
    }

    @Operation(summary = "微信小程序登录(Code)")
    @PostMapping("/wx/miniapp/code-login")
    public Result<AuthenticationToken> loginByWxMiniAppCode(@RequestBody @Valid WxMiniAppCodeLoginDTO loginDTO) {
        AuthenticationToken token = authService.loginByWxMiniAppCode(loginDTO);
        return Result.success(token);
    }

    @Operation(summary = "微信小程序登录(手机号)")
    @PostMapping("/wx/miniapp/phone-login")
    public Result<AuthenticationToken> loginByWxMiniAppPhone(@RequestBody @Valid WxMiniAppPhoneLoginDTO loginDTO) {
        AuthenticationToken token = authService.loginByWxMiniAppPhone(loginDTO);
        return Result.success(token);
    }


    @Operation(summary = "退出登录")
    @DeleteMapping("/logout")
    @Log(value = "退出登录", module = LogModuleEnum.LOGIN)
    public Result<?> logout() {
        authService.logout();
        return Result.success();
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh-token")
    public Result<?> refreshToken(
            @Parameter(description = "刷新令牌", example = "xxx.xxx.xxx") @RequestParam String refreshToken
    ) {
        AuthenticationToken authenticationToken = authService.refreshToken(refreshToken);
        return Result.success(authenticationToken);
    }

}
