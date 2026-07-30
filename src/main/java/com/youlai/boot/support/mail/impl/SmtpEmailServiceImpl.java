package com.youlai.boot.support.mail.impl;

import com.youlai.boot.config.property.MailProperties;
import com.youlai.boot.support.mail.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * SMTP 邮件服务。
 * <p>
 * 当 {@code mail.type=smtp}（默认）时生效。基于 Spring {@link JavaMailSender} 发送邮件。
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
    /** 发件人地址等邮件配置，来自 {@code mail.*} */
    private final MailProperties mailProperties;

    /**
     * 发送纯文本邮件，发件人取自 {@code mail.from} 配置。
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
     * 发送带附件的 HTML 邮件。
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
