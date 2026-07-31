package com.youlai.boot.framework.security.port;

import com.youlai.boot.common.enums.SocialPlatformEnum;
import com.youlai.boot.framework.security.model.SecurityUser;

/**
 * 用户认证信息查询端口。
 * <p>
 * 由 system 模块提供适配器实现，framework 层通过此接口获取认证数据，
 * 不直接依赖 system 模块的 {@code UserService} / {@code UserSocialService}。
 *
 * @see com.youlai.boot.system.security.adapter.UserAuthenticationAdapter
 */
public interface UserAuthenticationPort {

    /**
     * 根据用户名查询认证信息。
     *
     * @param username 用户名
     * @return 认证信息，不存在返回 null
     */
    SecurityUser getAuthInfoByUsername(String username);

    /**
     * 根据手机号查询认证信息。
     *
     * @param mobile 手机号
     * @return 认证信息，不存在返回 null
     */
    SecurityUser getAuthInfoByMobile(String mobile);

    /**
     * 根据第三方平台 openid 查询认证信息。
     *
     * @param platform 第三方平台
     * @param openid   openid
     * @return 认证信息，未绑定返回 null
     */
    SecurityUser getAuthInfoByOpenid(SocialPlatformEnum platform, String openid);
}
