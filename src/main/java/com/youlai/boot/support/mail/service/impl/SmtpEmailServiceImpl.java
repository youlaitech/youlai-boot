package com.youlai.boot.support.mail.service.impl;

import com.youlai.boot.support.mail.MailProperties;
import com.youlai.boot.support.mail.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * SMTP 邮件服务
 *
 * @author Ray.Hao
 * @since 3.1.0
 */
@Slf4j
@Service
@ConditionalOnProperty(value = "mail.type", havingValue = "smtp", matchIfMissing = true)
@RequiredArgsConstructor
public class SmtpEmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    // 发件人地址等邮件配置，绑定 spring.mail.*
    private final MailProperties mailProperties;

    /**
     * 发送纯文本邮件，发件人取自 {@code spring.mail.from} 配置
     */
    @Override
    public void send(String to, String subject, String text) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(mailProperties.getFrom());
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(text);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("发送邮件失败: {}", e.getMessage());
        }
    }

    /**
     * 发送HTML格式的邮件
     *
     * @param to      收件人地址
     * @param subject 邮件主题
     * @param html    html格式的内容
     */
    @Override
    public void sendHtmlMail(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // 设置发件人、收件人和主题
            message.setFrom(mailProperties.getFrom());
            message.setRecipients(jakarta.mail.Message.RecipientType.TO, to);
            message.setSubject(subject, StandardCharsets.UTF_8.name());
            // 创建 MimeMultipart 容器
            MimeMultipart multipart = new MimeMultipart("related");
            // 创建 HTML 内容部分
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(html, "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);
            // 设置消息内容
            message.setContent(multipart);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("发送Html邮件失败{}", e.getMessage());
        }
    }

    /**
     * 发送带附件的 HTML 邮件
     */
    @Override
    public void sendWithAttachment(String to, String subject, String text, String filePath) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(mailProperties.getFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true);

            FileSystemResource file = new FileSystemResource(new File(filePath));
            helper.addAttachment(file.getFilename(), file);

            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("发送带附件邮件失败: {}", e.getMessage());
        }
    }
}
