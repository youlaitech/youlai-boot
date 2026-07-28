package com.youlai.boot.auth.qrcode.controller;

import cn.hutool.core.util.StrUtil;
import com.youlai.boot.auth.qrcode.model.form.QrCodeTicketForm;
import com.youlai.boot.auth.qrcode.model.vo.QrCodeGenerateVO;
import com.youlai.boot.auth.qrcode.model.vo.QrCodeStatusVO;
import com.youlai.boot.auth.qrcode.service.QrCodeLoginService;
import com.youlai.boot.framework.annotation.RateLimit;
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
 * 扫码登录接口。
 * <p>
 * generate/status/login 不需要登录态（PC 端未登录），由 Security 配置放行；
 * scan/confirm/cancel 需要 APP 端登录态，当前用户 ID 从 Security 上下文获取。
 */
@Tag(name = "02.扫码登录")
@RestController
@RequestMapping("/api/v1/auth/qr-code")
@RequiredArgsConstructor
public class QrCodeLoginController {

    private final QrCodeLoginService qrCodeLoginService;

    @Operation(summary = "生成扫码登录票据")
    @PostMapping("/generate")
    @RateLimit(limit = 30, window = 60)
    public Result<QrCodeGenerateVO> generate(HttpServletRequest request) {
        return Result.success(qrCodeLoginService.generate(getClientIp(request)));
    }

    @Operation(summary = "查询扫码状态")
    @GetMapping("/status")
    @RateLimit(limit = 60, window = 60)
    public Result<QrCodeStatusVO> status(@RequestParam String ticket) {
        return Result.success(qrCodeLoginService.status(ticket));
    }

    @Operation(summary = "APP 标记已扫码")
    @PostMapping("/scan")
    public Result<QrCodeStatusVO> scan(@RequestBody @Valid QrCodeTicketForm form) {
        return Result.success(qrCodeLoginService.scan(form.getTicket(), SecurityUtils.getUserId()));
    }

    @Operation(summary = "APP 确认登录")
    @PostMapping("/confirm")
    public Result<QrCodeStatusVO> confirm(@RequestBody @Valid QrCodeTicketForm form) {
        return Result.success(qrCodeLoginService.confirm(form.getTicket(), SecurityUtils.getUserId()));
    }

    @Operation(summary = "APP 取消登录")
    @PostMapping("/cancel")
    public Result<QrCodeStatusVO> cancel(@RequestBody @Valid QrCodeTicketForm form) {
        return Result.success(qrCodeLoginService.cancel(form.getTicket(), SecurityUtils.getUserId()));
    }

    @Operation(summary = "PC 端换取会话令牌")
    @PostMapping("/login")
    public Result<AuthenticationToken> login(@RequestBody @Valid QrCodeTicketForm form) {
        return Result.success(qrCodeLoginService.login(form.getTicket()));
    }

    /** 从请求头或连接信息中提取客户端 IP，兼容反向代理 */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            int comma = ip.indexOf(',');
            return comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }
}
