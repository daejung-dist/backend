package shop.daejung.daejungdistbackend.global.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Component
public class EmailSender {
    private static final String SENDER_NAME = "대정유통";

    private final JavaMailSender mailSender;
    // Gmail은 로그인한 계정 주소로만 보낼 수 있어서 보내는 주소로 계정 주소를 그대로 쓴다
    private final String from;

    public EmailSender(JavaMailSender mailSender, @Value("${spring.mail.username}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void send(String to, String subject, String text) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            // 한글 제목과 본문이 깨지지 않게 UTF-8로 만든다
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from, SENDER_NAME);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailPreparationException("메일을 만들지 못했습니다.", e);
        }
        mailSender.send(message);
    }
}
