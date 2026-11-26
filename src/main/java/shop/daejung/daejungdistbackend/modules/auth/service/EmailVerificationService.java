package shop.daejung.daejungdistbackend.modules.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import shop.daejung.daejungdistbackend.global.mail.EmailSender;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final String CODE_KEY_PREFIX = "email-verification:code:";
    private static final String SUBJECT = "[대정유통] 회원가입 인증번호";

    private final StringRedisTemplate redisTemplate;
    private final EmailSender emailSender;
    // 인증번호는 예측할 수 없어야 하므로 Random 대신 SecureRandom을 쓴다
    private final SecureRandom secureRandom = new SecureRandom();

    public void sendCode(String rawEmail) {
        String email = normalize(rawEmail);
        String code = generateCode();

        // 다시 요청하면 이전 번호를 덮어써서 마지막에 받은 번호만 쓸 수 있게 한다
        redisTemplate.opsForValue().set(codeKey(email), code, CODE_TTL);

        try {
            emailSender.send(email, SUBJECT, buildText(code));
        } catch (MailException e) {
            // 메일이 가지 않았으면 아무도 모르는 번호가 남지 않게 지운다
            redisTemplate.delete(codeKey(email));
            // 예외 메시지에 받는 주소가 들어갈 수 있어서 예외 종류만 남긴다
            log.error("인증 메일 발송 실패: {}", e.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "인증 메일을 보내지 못했습니다.");
        }
    }

    private String generateCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String buildText(String code) {
        return """
                대정유통 회원가입 인증번호는 [%s] 입니다.
                5분 안에 입력해 주세요.

                본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.
                """.formatted(code);
    }

    // 대소문자만 다른 주소를 같은 이메일로 본다
    private String normalize(String email) {
        return email.toLowerCase(Locale.ROOT);
    }

    private String codeKey(String email) {
        return CODE_KEY_PREFIX + email;
    }
}
