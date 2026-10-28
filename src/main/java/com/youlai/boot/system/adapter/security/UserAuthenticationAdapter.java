package com.youlai.boot.system.adapter.security;

import com.youlai.boot.framework.security.model.SecurityUser;
import com.youlai.boot.framework.security.port.UserAuthenticationPort;
import com.youlai.boot.system.service.UserIdentityService;
import com.youlai.boot.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 用户认证信息查询适配器
 *
 * @author Ray.Hao
 * @since 3.1.0
 * @see UserAuthenticationPort
 */
@Component
@RequiredArgsConstructor
public class UserAuthenticationAdapter implements UserAuthenticationPort {

    private final UserService userService;
    private final UserIdentityService userIdentityService;

    @Override
    public SecurityUser getAuthInfoByUserId(Long userId) {
        return userIdentityService.getAuthInfoByUserId(userId);
    }

    @Override
    public SecurityUser getAuthInfoByUsername(String username) {
        return userService.getAuthInfoByUsername(username);
    }

    @Override
    public SecurityUser getAuthInfoByMobile(String mobile) {
        return userService.getAuthInfoByMobile(mobile);
    }

    @Override
    public SecurityUser getAuthInfoByProviderAccount(String provider, String providerAccountId) {
        return userIdentityService.getAuthInfoByProviderAccount(provider, providerAccountId);
    }
}
