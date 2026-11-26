package shop.daejung.daejungdistbackend.modules.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import shop.daejung.daejungdistbackend.global.mail.EmailSender;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration VERIFIED_TOKEN_TTL = Duration.ofMinutes(30);
    private static final int MAX_ATTEMPTS = 5;
    private static final String CODE_KEY_PREFIX = "email-verification:code:";
    private static final String ATTEMPTS_KEY_PREFIX = "email-verification:attempts:";
    private static final String TOKEN_KEY_PREFIX = "email-verification:token:";
    private static final String SUBJECT = "[대정유통] 회원가입 인증번호";

    private final StringRedisTemplate redisTemplate;
    private final EmailSender emailSender;
    // 인증번호는 예측할 수 없어야 하므로 Random 대신 SecureRandom을 쓴다
    private final SecureRandom secureRandom = new SecureRandom();

//    인증번호 발송
    public void sendCode(String email) {
        String code = generateCode();

        // 다시 요청하면 이전 번호를 덮어써서 마지막에 받은 번호만 쓸 수 있게 한다
        redisTemplate.opsForValue().set(codeKey(email), code, CODE_TTL);
        // 새 번호에는 입력 기회를 다시 준다
        redisTemplate.delete(attemptsKey(email));

        try {
            emailSender.send(email, SUBJECT, buildText(code));
        } catch (MailException e) {
            // 메일이 가지 않았으면 아무도 모르는 번호가 남지 않게 지운다
            deleteCode(email);
            // 예외 메시지에 받는 주소가 들어갈 수 있어서 예외 종류만 남긴다
            log.error("인증 메일 발송 실패: {}", e.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "인증 메일을 보내지 못했습니다.");
        }
    }

//    인증번호 확인
    public String confirm(String email, String code) {
        String savedCode = redisTemplate.opsForValue().get(codeKey(email));
        if (savedCode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증번호가 만료되었습니다. 다시 받아 주세요.");
        }

        // 맞는지 보기 전에 횟수부터 세야 동시에 여러 번 보내도 5번을 넘겨 시도할 수 없다
        long attempts = incrementAttempts(email);
        if (attempts > MAX_ATTEMPTS) {
            deleteCode(email);
            throw tooManyAttempts();
        }
        if (!matches(savedCode, code)) {
            // 마지막 기회까지 틀리면 번호를 없애서 새로 받게 한다
            if (attempts == MAX_ATTEMPTS) {
                deleteCode(email);
                throw tooManyAttempts();
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증번호가 일치하지 않습니다.");
        }

        deleteCode(email);
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(tokenKey(token), email, VERIFIED_TOKEN_TTL);
        return token;
    }

//    회원가입
    // 토큰은 가입 한 번에만 쓸 수 있도록 꺼내면서 지운다
    public void consumeVerifiedEmail(String token, String email) {
        String verifiedEmail = redisTemplate.opsForValue().getAndDelete(tokenKey(token));
        if (!email.equals(verifiedEmail)) {
            // 400은 입력값 오류와 구분되지 않아서, 프론트가 인증을 다시 시작하도록 422로 알린다
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "이메일 인증이 만료되었습니다. 다시 인증해 주세요.");
        }
    }

    private long incrementAttempts(String email) {
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey(email));
        if (attempts == 1) {
            redisTemplate.expire(attemptsKey(email), CODE_TTL);
        }
        return attempts;
    }

    // 비교에 걸리는 시간으로 번호를 짐작하지 못하게 항상 같은 시간이 걸리는 비교를 쓴다
    private boolean matches(String savedCode, String code) {
        return MessageDigest.isEqual(
                savedCode.getBytes(StandardCharsets.UTF_8),
                code.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void deleteCode(String email) {
        redisTemplate.delete(List.of(codeKey(email), attemptsKey(email)));
    }

    private ResponseStatusException tooManyAttempts() {
        return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "인증번호 입력 횟수를 초과했습니다. 다시 받아 주세요.");
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

    private String codeKey(String email) {
        return CODE_KEY_PREFIX + email;
    }

    private String attemptsKey(String email) {
        return ATTEMPTS_KEY_PREFIX + email;
    }

    private String tokenKey(String token) {
        return TOKEN_KEY_PREFIX + token;
    }
}
