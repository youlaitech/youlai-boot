package com.youlai.boot.common.constant;

/**
 * Redis 常量
 *
 * @author Theo
 * @since 2024-7-29 11:46:08
 */
public interface RedisConstants {

    /**
     * 限流相关键
     */
    interface RateLimiter {
        String IP = "rate_limiter:ip:{}"; // IP限流（示例：rate_limiter:ip:192.168.1.1）
    }

    /**
     * 分布式锁相关键
     */
    interface Lock {
        String RESUBMIT = "lock:resubmit:{}:{}"; // 防重复提交（示例：lock:resubmit:userIdentifier:requestIdentifier）
    }

    /**
     * 认证模块
     */
    interface Auth {
        // 存储访问令牌对应的用户会话信息（accessToken -> UserSession）
        String ACCESS_TOKEN_USER = "auth:token:access:{}";
        // 存储刷新令牌对应的用户会话信息（refreshToken -> UserSession）
        String REFRESH_TOKEN_USER = "auth:token:refresh:{}";
        // 用户与访问令牌的映射（userId -> accessToken）
        String USER_ACCESS_TOKEN = "auth:user:access:{}";
        // 用户与刷新令牌的映射（userId -> refreshToken
        String USER_REFRESH_TOKEN = "auth:user:refresh:{}";
        // 已撤销 Token 的 JTI（单端退出/会话注销）：如果 jti 在撤销列表中，则 Token 立即无效
        String BLACKLIST_TOKEN = "auth:token:blacklist:{}";
        String REVOKED_JTI = BLACKLIST_TOKEN;
        // 用户 Token 生效起点（用于按用户失效历史 JWT）：token.iat < tokenValidAfter => token 无效
        String USER_TOKEN_VALID_AFTER = "auth:user:token_valid_after:{}";
    }

    /**
     * 验证码模块
     */
    interface Captcha {
        String IMAGE_CODE = "captcha:image:{}";              // 图形验证码
        String SMS_LOGIN_CODE = "captcha:sms_login:{}";      // 登录短信验证码
        String SMS_REGISTER_CODE = "captcha:sms_register:{}";// 注册短信验证码
        String MOBILE_CODE = "captcha:mobile:{}";            // 绑定、更换手机验证码
        String EMAIL_CODE = "captcha:email:{}";              // 邮箱验证码
    }

    /**
     * 系统模块
     */
    interface System {
        String CONFIG = "system:config";                 // 系统配置
        String ROLE_PERMS = "system:role:perms"; // 系统角色和权限映射
    }

}
