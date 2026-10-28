package com.youlai.boot.framework.security.port;

import com.youlai.boot.framework.security.model.SecurityUser;

/**
 * 用户认证信息查询端口
 *
 * @see com.youlai.boot.system.adapter.security.UserAuthenticationAdapter
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
public interface UserAuthenticationPort {

    /**
     * 根据稳定的用户 ID 重新装载账号状态和权限
     */
    SecurityUser getAuthInfoByUserId(Long userId);

    /**
     * 根据用户名查询认证信息
     *
     * @param username 用户名
     * @return 认证信息，不存在返回 null
     */
    SecurityUser getAuthInfoByUsername(String username);

    /**
     * 根据手机号查询认证信息
     *
     * @param mobile 手机号
     * @return 认证信息，不存在返回 null
     */
    SecurityUser getAuthInfoByMobile(String mobile);

    /**
     * 根据具体认证源和来源方账号 ID 查询认证信息
     *
     * @param provider          固定认证源标识，不是协议类型
     * @param providerAccountId 来源方账号 ID
     * @return 认证信息，未绑定返回 null
     */
    SecurityUser getAuthInfoByProviderAccount(String provider, String providerAccountId);
}
