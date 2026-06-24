package com.youlai.boot.framework.integration.mail.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 邮件配置属性
 *
 * @author Ray
 * @since 2024/8/17
 */
@ConfigurationProperties(prefix = "spring.mail")
@Data
public class MailProperties {

    /** SMTP 服务器地址 */
    private String host;

    /** SMTP 端口 */
    private int port;

    /** 发件人账号 */
    private String username;

    /** 发件人密码 */
    private String password;

    /** 发件人地址 */
    private String from;

    /** SMTP 扩展属性 */
    private Properties properties = new Properties();

    @Data
    public static class Properties {
        private Smtp smtp = new Smtp();

        @Data
        public static class Smtp {
            /** 启用 SMTP 认证 */
            private boolean auth;

            private StartTls starttls = new StartTls();

            @Data
            public static class StartTls {
                /** 启用 STARTTLS */
                private boolean enable;
            }
        }
    }
}
