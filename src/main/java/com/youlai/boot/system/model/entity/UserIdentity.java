package com.youlai.boot.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 本地用户与外部账号的关联
 *
 * <p>provider 是具体认证源的固定配置标识，不是协议类型。例如两个小程序使用不同 provider；
 * 同一 provider 不得在保留关联数据的情况下更换 issuer 或 AppID
 *
 * @author Ray.Hao
 * @since 4.13.0
 */
@TableName("sys_user_identity")
@Getter
@Setter
public class UserIdentity {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 承载本地角色、部门与数据权限的用户 ID
    private Long userId;

    // 具体认证源标识，例如 oidc-main、wechat-mini-main、github
    private String provider;

    // 来源方账号 ID：OIDC sub、微信 OpenID 或 GitHub/Gitee 用户 ID
    private String providerAccountId;

    // 外部账号展示名；只作显示，不参与身份匹配
    private String displayName;

    // 外部账号头像；不覆盖本地用户资料
    private String avatarUrl;

    // 最近一次通过该外部账号登录的时间
    private LocalDateTime lastLoginTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
