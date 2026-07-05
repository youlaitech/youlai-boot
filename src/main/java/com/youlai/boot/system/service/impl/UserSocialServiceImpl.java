package com.youlai.boot.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.framework.security.model.SecurityUser;
import com.youlai.boot.common.enums.SocialPlatformEnum;
import com.youlai.boot.system.mapper.UserSocialMapper;
import com.youlai.boot.system.model.entity.UserSocial;
import com.youlai.boot.system.service.UserSocialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户第三方账号绑定业务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSocialServiceImpl extends ServiceImpl<UserSocialMapper, UserSocial> implements UserSocialService {

    @Override
    public UserSocial getByPlatformAndOpenid(SocialPlatformEnum platform, String openid) {
        return getOne(new LambdaQueryWrapper<UserSocial>()
                .eq(UserSocial::getPlatform, platform)
                .eq(UserSocial::getOpenid, openid));
    }

    @Override
    public UserSocial getByUnionid(String unionid) {
        if (StrUtil.isBlank(unionid)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<UserSocial>()
                .eq(UserSocial::getUnionid, unionid));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserSocial bindOrUpdate(Long userId, SocialPlatformEnum platform, String openid, String unionid, String nickname, String avatar, String sessionKey) {
        UserSocial userSocial = getByPlatformAndOpenid(platform, openid);
        LocalDateTime now = LocalDateTime.now();

        if (userSocial == null) {
            userSocial = new UserSocial();
            userSocial.setUserId(userId);
            userSocial.setPlatform(platform);
            userSocial.setOpenid(openid);
            userSocial.setUnionid(unionid);
            userSocial.setNickname(nickname);
            userSocial.setAvatar(avatar);
            userSocial.setSessionKey(sessionKey);
            userSocial.setVerified(1);
            userSocial.setCreateTime(now);
            userSocial.setUpdateTime(now);
            save(userSocial);
            log.info("第三方账号绑定成功：userId={}, platform={}, openid={}", userId, platform, openid);
        } else {
            userSocial.setUserId(userId);
            userSocial.setUnionid(unionid);
            userSocial.setNickname(nickname);
            userSocial.setAvatar(avatar);
            userSocial.setSessionKey(sessionKey);
            userSocial.setUpdateTime(now);
            updateById(userSocial);
            log.info("第三方账号更新成功：userId={}, platform={}, openid={}", userId, platform, openid);
        }

        return userSocial;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unbind(Long userId, SocialPlatformEnum platform) {
        boolean removed = remove(new LambdaQueryWrapper<UserSocial>()
                .eq(UserSocial::getUserId, userId)
                .eq(UserSocial::getPlatform, platform));
        if (removed) {
            log.info("第三方账号解绑成功：userId={}, platform={}", userId, platform);
        }
        return removed;
    }

    @Override
    public SecurityUser getAuthInfoByOpenid(SocialPlatformEnum platform, String openid) {
        UserSocial userSocial = getByPlatformAndOpenid(platform, openid);
        if (userSocial == null) {
            return null;
        }
        return baseMapper.getAuthInfoByUserId(userSocial.getUserId());
    }

    @Override
    public void updateSessionKey(Long id, String sessionKey) {
        UserSocial userSocial = getById(id);
        if (userSocial != null) {
            userSocial.setSessionKey(sessionKey);
            userSocial.setUpdateTime(LocalDateTime.now());
            updateById(userSocial);
        }
    }

}
