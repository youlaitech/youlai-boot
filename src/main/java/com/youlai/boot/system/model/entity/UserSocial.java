package com.youlai.boot.system.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;
import com.youlai.boot.common.enums.SocialPlatformEnum;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户第三方账号绑定实体
 */
@TableName("sys_user_social")
@Getter
@Setter
public class UserSocial {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 平台类型
     */
    private SocialPlatformEnum platform;

    /**
     * 平台openid
     */
    private String openid;

    /**
     * 微信unionid
     */
    private String unionid;

    /**
     * 第三方昵称
     */
    private String nickname;

    /**
     * 第三方头像URL
     */
    private String avatar;

    /**
     * 微信session_key
     */
    private String sessionKey;

    /**
     * 是否已验证(1-已验证 0-未验证)
     */
    private Integer verified;

    /**
     * 绑定时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
