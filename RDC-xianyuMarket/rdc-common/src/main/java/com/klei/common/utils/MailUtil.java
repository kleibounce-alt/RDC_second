package com.klei.common.utils;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MailUtil {

    private static final String FROM_EMAIL;
    private static final String AUTH_CODE;
    private static final String SMTP_HOST;
    private static final String SMTP_PORT;
    private static final Session SESSION;

    static {
        Properties cfg = new Properties();
        try (InputStream is = MailUtil.class.getClassLoader().getResourceAsStream("mail.properties")) {
            if (is != null) {
                cfg.load(is);
            }
        } catch (IOException e) {
            System.err.println("[MailUtil] mail.properties 未找到，使用默认值");
        }

        FROM_EMAIL = cfg.getProperty("mail.from", "3511396958@qq.com");
        AUTH_CODE   = cfg.getProperty("mail.auth", "oidspovtnqqrchfe");
        SMTP_HOST   = cfg.getProperty("mail.host", "smtp.qq.com");
        SMTP_PORT   = cfg.getProperty("mail.port", "587");

        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");

        SESSION = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, AUTH_CODE);
            }
        });
    }

    public static void sendResetCode(String to, String code) {
        String subject = "密码重置验证码";
        String html = "<p>您的验证码为：<b style='color:red;font-size:20px'>" + code + "</b></p>"
                + "<p>10 分钟内有效，请勿告知他人。</p>";
        new Thread(() -> doSend(to, subject, html, true)).start();
    }

    private static void doSend(String to, String subject, String content, boolean isHtml) {
        try {
            Message msg = new MimeMessage(SESSION);
            msg.setFrom(new InternetAddress(FROM_EMAIL));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            msg.setSubject(subject);
            if (isHtml) {
                msg.setContent(content, "text/html;charset=UTF-8");
            } else {
                msg.setText(content);
            }
            Transport.send(msg);
        } catch (MessagingException e) {
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        }
    }
}