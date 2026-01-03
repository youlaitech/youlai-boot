package com.youlai.boot.security.provider;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.youlai.boot.security.model.SysUserDetails;
import com.youlai.boot.security.model.UserAuthInfo;
import com.youlai.boot.security.model.WxMiniAppCodeAuthenticationToken;
import com.youlai.boot.system.service.UserService;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * 微信小程序Code认证Provider
 *
 * @author 有来技术团队
 * @since 2.0.0
 */
@Slf4j
public class WxMiniAppCodeAuthenticationProvider implements AuthenticationProvider {

    private final UserService userService;
    private final WxMaService wxMaService;

    public WxMiniAppCodeAuthenticationProvider(UserService userService, WxMaService wxMaService) {
        this.userService = userService;
        this.wxMaService = wxMaService;
    }

    /**
     * 微信认证逻辑，参考 Spring Security 认证密码校验流程
     *
     * @param authentication 认证对象
     * @return 认证后的 Authentication 对象
     * @throws AuthenticationException 认证异常
     * @see org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider#authenticate(Authentication)
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String code = (String) authentication.getPrincipal();

        // 通过微信服务端验证 code 并获取用户会话信息
        WxMaJscode2SessionResult sessionInfo;
        try {
            sessionInfo = wxMaService.getUserService().getSessionInfo(code);
        } catch (WxErrorException e) {
            throw new CredentialsExpiredException("微信登录 code 无效或已失效，请重新获取");
        }

        String openId = sessionInfo.getOpenid();
        if (StrUtil.isBlank(openId)) {
            throw new UsernameNotFoundException("未能获取到微信 OpenID，请稍后重试");
        }

        // 根据微信 OpenID 查询用户信息
        UserAuthInfo userAuthInfo = userService.getAuthInfoByOpenId(openId);

        if (userAuthInfo == null) {
            // 用户不存在则注册
            userService.registerOrBindWechatUser(openId);

            // 再次查询用户信息，确保用户注册成功
            userAuthInfo = userService.getAuthInfoByOpenId(openId);
            if (userAuthInfo == null) {
                throw new UsernameNotFoundException("用户注册失败，请稍后重试");
            }
        }

        // 检查用户状态是否有效
        if (ObjectUtil.notEqual(userAuthInfo.getStatus(), 1)) {
            throw new DisabledException("用户已被禁用");
        }

        // 构建认证后的用户详情信息
        SysUserDetails userDetails = new SysUserDetails(userAuthInfo);

        // 创建已认证的Token
        return WxMiniAppCodeAuthenticationToken.authenticated(
                userDetails,
                userDetails.getAuthorities()
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return WxMiniAppCodeAuthenticationToken.class.isAssignableFrom(authentication);
    }
} 