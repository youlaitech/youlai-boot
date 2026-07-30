package com.youlai.boot.support.mail;

/**
 * 邮件发送服务接口。
 * <p>
 * 由各邮件供应商实现（SMTP、SendGrid 等），通过 {@code mail.type} 配置项选择。
 *
 * @author Ray.Hao
 * @since 3.1.0
 */
public interface EmailService {

    /**
     * 发送纯文本邮件。
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param text    邮件正文
     */
    void send(String to, String subject, String text);

    /**
     * 发送带附件的邮件。
     *
     * @param to       收件人邮箱
     * @param subject  邮件主题
     * @param text     邮件正文（支持 HTML）
     * @param filePath 附件文件绝对路径
     */
    void sendWithAttachment(String to, String subject, String text, String filePath);

}
