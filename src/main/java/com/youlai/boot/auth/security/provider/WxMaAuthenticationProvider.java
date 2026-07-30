package com.youlai.boot.auth.security.provider;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.util.ObjectUtil;
import com.youlai.boot.common.enums.SocialPlatformEnum;
import com.youlai.boot.framework.security.model.SecurityUser;
import com.youlai.boot.framework.security.model.SecurityUserDetails;
import com.youlai.boot.framework.security.port.UserAuthenticationPort;
import com.youlai.boot.common.exception.MobileNotBoundException;
import com.youlai.boot.auth.security.token.WxMaAuthenticationToken;
import com.youlai.boot.system.model.entity.UserSocial;
import com.youlai.boot.system.service.UserSocialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * 微信小程序认证 Provider
 */
@Slf4j
@RequiredArgsConstructor
public class WxMaAuthenticationProvider implements AuthenticationProvider {

    private final WxMaService wxMaService;
    private final UserAuthenticationPort userAuthPort;
    private final UserSocialService userSocialService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String code = (String) authentication.getPrincipal();

        if (code == null || code.isEmpty()) {
            log.warn("微信小程序登录失败：code为空");
            throw new IllegalArgumentException("code不能为空");
        }

        try {
            WxMaJscode2SessionResult session = wxMaService.jsCode2SessionInfo(code);
            String openid = session.getOpenid();
            String sessionKey = session.getSessionKey();

            log.info("微信小程序登录：openid={}", openid);

            UserSocial userSocial = userSocialService.getByPlatformAndOpenid(SocialPlatformEnum.WECHAT_MINI, openid);

            if (userSocial == null) {
                log.info("微信小程序登录：用户未绑定手机号，openid={}", openid);
                throw new MobileNotBoundException(openid, sessionKey);
            }

            SecurityUser securityUser = userAuthPort.getAuthInfoByOpenid(SocialPlatformEnum.WECHAT_MINI, openid);

            if (securityUser == null) {
                log.warn("微信小程序登录失败：用户不存在，openid={}", openid);
                throw new UsernameNotFoundException("用户不存在");
            }

            if (ObjectUtil.notEqual(securityUser.getStatus(), 1)) {
                log.warn("微信小程序登录失败：用户已禁用，username={}", securityUser.getUsername());
                throw new DisabledException("用户已被禁用");
            }

            userSocialService.updateSessionKey(userSocial.getId(), sessionKey);

            SecurityUserDetails userDetails = new SecurityUserDetails(securityUser);
            log.info("微信小程序登录成功：username={}, openid={}", securityUser.getUsername(), openid);
            return WxMaAuthenticationToken.authenticated(userDetails, userDetails.getAuthorities());

        } catch (WxErrorException e) {
            log.error("微信小程序登录失败：调用微信接口异常，code={}", code, e);
            throw new IllegalArgumentException("微信登录失败：" + e.getMessage());
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return WxMaAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
