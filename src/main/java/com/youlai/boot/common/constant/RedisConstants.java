package com.youlai.boot.common.constant;

/**
 * Redis 常量
 *
 * @author Theo
 * @since 4.3.1
 */
public interface RedisConstants {

    /**
     * 限流相关键
     */
    interface RateLimit {
        // 接口级限流 Key（示例：rate_limit:api:{user}:{uri}）
        String API = "rate_limit:api:{}:{}";

        // IP 全局限流 Key（示例：rate_limit:ip:192.168.1.100）
        String IP = "rate_limit:ip:{}";
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
        // 扫码登录票据上下文（ticket -> QrCodeLoginContext）
        String QR_CODE_LOGIN = "auth:qr_code:v2:{}";
        // 统一认证一次性交接票据（ticket -> OidcTicketContext）
        String OIDC_TICKET = "auth:oidc:ticket:v2:{}";
    }

    /**
     * 微信小程序模块
     */
    interface WxMa {
        // 微信会话密钥（provider + OpenID -> session_key），不同小程序分别存储
        String SESSION_KEY = "auth:wxma:session_key:{}:{}";
        long SESSION_KEY_TTL_DAYS = 3;
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

    /**
     * 表单模块
     */
    interface Form {
        // 已发布表单渲染规则（formKey -> FormRenderVO，发布/停用/改规则/删除时主动失效）
        String RENDER_RULE = "form:render:{}";
        // 公开表单匿名提交计数（formKey:ip 维度滑动窗口，防刷限流）
        String PUBLIC_SUBMIT_COUNT = "form:public:submit:{}:{}";
    }

}
