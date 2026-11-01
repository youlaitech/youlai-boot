package com.youlai.boot.support.mail.service;

/**
 * 邮件发送服务接口
 *
 * @author Ray.Hao
 * @since 3.1.0
 */
public interface EmailService {

    /**
     * 发送纯文本邮件
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param text    邮件正文
     */
    void send(String to, String subject, String text);

    /**
     * 发送HTML格式的邮件
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param html    邮件正文（HTML 格式）
     */
    void sendHtmlMail(String to, String subject, String html);

    /**
     * 发送带附件的邮件
     *
     * @param to       收件人邮箱
     * @param subject  邮件主题
     * @param text     邮件正文（支持 HTML）
     * @param filePath 附件文件绝对路径
     */
    void sendWithAttachment(String to, String subject, String text, String filePath);

}