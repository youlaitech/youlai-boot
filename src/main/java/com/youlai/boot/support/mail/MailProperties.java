package com.youlai.boot.support.mail;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 邮件配置属性；默认使用 587 端口、SMTP 认证和 STARTTLS
 *
 * @author Ray
 * @since 2024/8/17
 */
@Component
@ConfigurationProperties(prefix = "spring.mail")
@Data
public class MailProperties {

    /** SMTP 服务器地址 */
    private String host;

    /** SMTP 端口 */
    private int port = 587;

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
            private boolean auth = true;

            private StartTls starttls = new StartTls();

            @Data
            public static class StartTls {
                /** 启用 STARTTLS */
                private boolean enable = true;
            }
        }
    }
}
