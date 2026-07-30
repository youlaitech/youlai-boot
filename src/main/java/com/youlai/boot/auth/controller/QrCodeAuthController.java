package com.youlai.boot.auth.controller;

import com.youlai.boot.auth.model.req.QrCodeTicketReq;
import com.youlai.boot.auth.model.resp.QrCodeGenerateResp;
import com.youlai.boot.auth.model.resp.QrCodeStatusResp;
import com.youlai.boot.auth.service.QrCodeLoginService;
import com.youlai.boot.common.util.IPUtils;
import com.youlai.boot.common.annotation.RateLimit;
import com.youlai.boot.common.result.Result;
import com.youlai.boot.framework.security.model.AuthenticationToken;
import com.youlai.boot.framework.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 扫码登录认证接口
 *
 * <p>generate/status/login 不需要登录态（PC 端未登录），由 Security 配置放行；
 * scan/confirm/cancel 需要 APP 端登录态，当前用户 ID 从 Security 上下文获取。</p>
 *
 * @author Ray.Hao
 * @since 4.5.0
 */
@Tag(name = "01.认证中心")
@RestController
@RequestMapping("/api/v1/auth/qr-code")
@RequiredArgsConstructor
public class QrCodeAuthController {

    private final QrCodeLoginService qrCodeLoginService;

    /**
     * 生成扫码登录票据
     */
    @Operation(summary = "[扫码]生成扫码登录票据")
    @PostMapping("/generate")
    @RateLimit(limit = 30, window = 60)
    public Result<QrCodeGenerateResp> generate(HttpServletRequest request) {
        return Result.success(qrCodeLoginService.generate(IPUtils.getIpAddr(request)));
    }

    /**
     * 查询扫码状态
     */
    @Operation(summary = "[扫码]查询扫码状态")
    @GetMapping("/status")
    @RateLimit(limit = 60, window = 60)
    public Result<QrCodeStatusResp> status(@RequestParam String ticket) {
        return Result.success(qrCodeLoginService.status(ticket));
    }

    /**
     * APP 标记已扫码
     */
    @Operation(summary = "[扫码]APP 标记已扫码")
    @PostMapping("/scan")
    public Result<QrCodeStatusResp> scan(@RequestBody @Valid QrCodeTicketReq form) {
        return Result.success(qrCodeLoginService.scan(form.getTicket(), SecurityUtils.getUserId()));
    }

    /**
     * APP 确认登录
     */
    @Operation(summary = "[扫码]APP 确认登录")
    @PostMapping("/confirm")
    public Result<QrCodeStatusResp> confirm(@RequestBody @Valid QrCodeTicketReq form) {
        return Result.success(qrCodeLoginService.confirm(form.getTicket(), SecurityUtils.getUserId()));
    }

    /**
     * APP 取消登录
     */
    @Operation(summary = "[扫码]APP 取消登录")
    @PostMapping("/cancel")
    public Result<QrCodeStatusResp> cancel(@RequestBody @Valid QrCodeTicketReq form) {
        return Result.success(qrCodeLoginService.cancel(form.getTicket(), SecurityUtils.getUserId()));
    }

    /**
     * PC 端用票据换取会话令牌
     */
    @Operation(summary = "[扫码]PC 端换取会话令牌")
    @PostMapping("/login")
    public Result<AuthenticationToken> login(@RequestBody @Valid QrCodeTicketReq form) {
        return Result.success(qrCodeLoginService.login(form.getTicket()));
    }
}
