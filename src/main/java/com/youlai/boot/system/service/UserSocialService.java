package com.youlai.boot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.youlai.boot.framework.security.model.SecurityUser;
import com.youlai.boot.common.enums.SocialPlatformEnum;
import com.youlai.boot.system.model.entity.UserSocial;

/**
 * 用户第三方账号绑定业务接口
 */
public interface UserSocialService extends IService<UserSocial> {

    /**
     * 根据平台和openid查询绑定信息
     *
     * @param platform 平台类型
     * @param openid   openid
     * @return 绑定信息
     */
    UserSocial getByPlatformAndOpenid(SocialPlatformEnum platform, String openid);

    /**
     * 根据unionid查询绑定信息
     *
     * @param unionid unionid
     * @return 绑定信息
     */
    UserSocial getByUnionid(String unionid);

    /**
     * 绑定或更新第三方账号
     *
     * @param userId     用户ID
     * @param platform   平台类型
     * @param openid     openid
     * @param unionid    unionid
     * @param nickname   昵称
     * @param avatar     头像
     * @param sessionKey session_key
     * @return 绑定信息
     */
    UserSocial bindOrUpdate(Long userId, SocialPlatformEnum platform, String openid, String unionid, String nickname, String avatar, String sessionKey);

    /**
     * 解绑第三方账号
     *
     * @param userId   用户ID
     * @param platform 平台类型
     * @return 是否成功
     */
    boolean unbind(Long userId, SocialPlatformEnum platform);

    /**
     * 根据openid获取用户认证信息
     *
     * @param platform 平台类型
     * @param openid   openid
     * @return 用户认证信息
     */
    SecurityUser getAuthInfoByOpenid(SocialPlatformEnum platform, String openid);

    /**
     * 更新session_key
     *
     * @param id         绑定记录ID
     * @param sessionKey session_key
     */
    void updateSessionKey(Long id, String sessionKey);

}
