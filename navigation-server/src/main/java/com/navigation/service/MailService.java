package com.navigation.service;

import jakarta.annotation.Resource;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


import javax.mail.MessagingException;
import java.util.Date;

@Service
public class MailService {
    @Value("${spring.mail.username}")
    private String mailUsername;

    @Resource
    private JavaMailSender javaMailSender;

    @Resource
    private TemplateEngine templateEngine;  // 使用 Thymeleaf 的 TemplateEngine

    public void sendMailForActivationAccount(String activationUrl, String email) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true);
            message.setSubject("激活账号");
            message.setFrom(mailUsername);
            message.setTo(email);
            message.setSentDate(new Date());

            // 准备 Thymeleaf 上下文数据
            Context context = new Context();
            context.setVariable("activationUrl", activationUrl);  // 设置激活码到模板中

            // 渲染模板（确保是正确的模板路径）
            String templateContent = "activation-account";  // Thymeleaf 模板文件的路径，不需要 .html 后缀
            String text = templateEngine.process(templateContent, context);  // 使用 Thymeleaf 渲染模板

            // 设置邮件正文（HTML 格式）
            message.setText(text, true);

        } catch (jakarta.mail.MessagingException e) {
            throw new RuntimeException("邮件发送失败", e);
        }

        // 发送邮件
        javaMailSender.send(mimeMessage);
    }
}