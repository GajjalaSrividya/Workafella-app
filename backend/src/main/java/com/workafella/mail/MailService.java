package com.workafella.mail;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
@Service
public class MailService {

    private static final Logger log =
            LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(String to,
                     String subject,
                     String body) {

        try {

            SimpleMailMessage msg = new SimpleMailMessage();

            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);

            mailSender.send(msg);

        } catch (Exception ex) {

            log.warn(
                    "Mail not sent to {}. Subject: {}",
                    to,
                    subject,
                    ex
            );
        }
    }

  public void sendHtml(String to,
                     String subject,
                     String htmlBody) {

    try {

        MimeMessage message =
                mailSender.createMimeMessage();

        MimeMessageHelper helper =
                new MimeMessageHelper(
                        message,
                        true,
                        "UTF-8"
                );

        helper.setTo(to);
        helper.setSubject(subject);

        helper.setText(htmlBody, true);

        ClassPathResource logo =
                new ClassPathResource(
                        "static/workafellaBG.png"
                );

        helper.addInline(
                "workafellaLogo",
                logo
        );

        mailSender.send(message);

    } catch (Exception ex) {

        log.error(
                "Failed to send HTML mail to {}",
                to,
                ex
        );
    }
}
}