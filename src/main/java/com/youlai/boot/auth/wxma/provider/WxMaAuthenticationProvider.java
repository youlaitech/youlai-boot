package com.youlai.boot.auth.wxma.provider;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.youlai.boot.auth.wxma.exception.MobileNotBoundException;
import com.youlai.boot.auth.wxma.config.WxMaProperties;
import com.youlai.boot.auth.wxma.token.WxMaAuthenticationToken;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.framework.security.model.SecurityUser;
import com.youlai.boot.framework.security.model.SecurityUserDetails;
import com.youlai.boot.framework.security.port.UserAuthenticationPort;
import com.youlai.boot.system.service.UserIdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.concurrent.TimeUnit;

/**
 * 微信小程序认证 Provider
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@Slf4j
@RequiredArgsConstructor
public class WxMaAuthenticationProvider implements AuthenticationProvider {

    private final WxMaService wxMaService;
    private final WxMaProperties wxMaProperties;
    private final UserAuthenticationPort userAuthPort;
    private final UserIdentityService userIdentityService;
    private final RedisTemplate<String, Object> redisTemplate;

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

            cacheSessionKey(openid, sessionKey);

            // provider 对应当前 AppID，同一个 OpenID 字符串不会跨小程序复用关联
            String provider = wxMaProperties.getProvider();
            SecurityUser securityUser = userAuthPort.getAuthInfoByProviderAccount(provider, openid);

            if (securityUser == null) {
                throw new MobileNotBoundException();
            }

            if (ObjectUtil.notEqual(securityUser.getStatus(), 1)) {
                log.warn("微信小程序登录失败：用户已禁用，username={}", securityUser.getUsername());
                throw new DisabledException("用户已被禁用");
            }

            SecurityUserDetails userDetails = new SecurityUserDetails(securityUser);
            userIdentityService.markLogin(provider, openid);
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

    /**
     * 会话密钥只存 Redis，不落业务库
     */
    private void cacheSessionKey(String openid, String sessionKey) {
        if (StrUtil.isBlank(openid) || StrUtil.isBlank(sessionKey)) {
            return;
        }
        redisTemplate.opsForValue().set(
                StrUtil.format(RedisConstants.WxMa.SESSION_KEY, wxMaProperties.getProvider(), openid),
                sessionKey,
                RedisConstants.WxMa.SESSION_KEY_TTL_DAYS,
                TimeUnit.DAYS);
    }
}
