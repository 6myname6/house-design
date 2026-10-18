package com.housedesign.Service.impl;

import java.io.UnsupportedEncodingException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.housedesign.Service.EmailService;
import com.housedesign.common.BusinessException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    @Value("${app.email.from-name}")
    private String fromName;

    // 私有方法：
    // 构造邮箱掩码
    private String maskEmail(String to) {
        String maskedEmail;
        maskedEmail = to.charAt(0) + "***" + to.substring(to.indexOf("@"), to.length());
        return maskedEmail;
    }

    @Override
    public void send(String target, String code) {
        if (from == null || from.isBlank()) {
            log.warn("邮箱名称未配置");
            throw new BusinessException(503, "邮件服务未配置，请联系管理员");
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            InternetAddress address = new InternetAddress(from, fromName, "UTF-8");
            helper.setFrom(address);
            helper.setTo(target);
            String htmlContent = """
                    <div style="max-width:420px;margin:0 auto;padding:32px;font-family:'Microsoft YaHei',Arial,sans-serif;">
                      <h2 style="margin:0 0 8px;color:#1a1a1a;">筑梦家</h2>
                      <p style="color:#666;font-size:14px;">您正在进行注册/登录操作，验证码为：</p>
                      <p style="margin:24px 0;font-size:32px;font-weight:bold;letter-spacing:8px;color:#b4533a;">"""
                    + code + """
                            </p>
                              <p style="color:#999;font-size:12px;">验证码 5 分钟内有效，请勿泄露给他人。如非本人操作，请忽略此邮件。</p>
                            </div>
                            """;
            helper.setSubject("【筑梦家】登录验证码");
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("验证码邮件已发送至{}", maskEmail(target));
        } catch (MessagingException | MailException | UnsupportedEncodingException e) {
            log.error("验证码发送失败，错误信息", e);
            throw new BusinessException(503, "验证码邮件发送失败，请稍后重试");
        }
    }

}
