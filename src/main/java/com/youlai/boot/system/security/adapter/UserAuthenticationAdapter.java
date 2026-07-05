package com.youlai.boot.system.security.adapter;

import com.youlai.boot.common.enums.SocialPlatformEnum;
import com.youlai.boot.framework.security.model.SecurityUser;
import com.youlai.boot.framework.security.port.UserAuthenticationPort;
import com.youlai.boot.system.service.UserService;
import com.youlai.boot.system.service.UserSocialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 用户认证信息查询适配器。
 * <p>
 * 实现 framework 层的 {@link UserAuthenticationPort}，委托 system 层服务完成查询。
 *
 * @see UserAuthenticationPort
 */
@Component
@RequiredArgsConstructor
public class UserAuthenticationAdapter implements UserAuthenticationPort {

    private final UserService userService;
    private final UserSocialService userSocialService;

    @Override
    public SecurityUser getAuthInfoByUsername(String username) {
        return userService.getAuthInfoByUsername(username);
    }

    @Override
    public SecurityUser getAuthInfoByMobile(String mobile) {
        return userService.getAuthInfoByMobile(mobile);
    }

    @Override
    public SecurityUser getAuthInfoByOpenid(SocialPlatformEnum platform, String openid) {
        return userSocialService.getAuthInfoByOpenid(platform, openid);
    }
}
