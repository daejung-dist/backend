package shop.daejung.daejungdistbackend.modules.auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import shop.daejung.daejungdistbackend.global.mail.EmailSender;
import shop.daejung.daejungdistbackend.modules.user.repository.UserRepository;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class EmailVerificationIntegrationTest {
    private static final Pattern CODE_PATTERN = Pattern.compile("\\[(\\d{6})]");

    // 운영과 같은 Redis 동작(TTL, GETDEL)을 확인하려고 진짜 Redis를 띄운다
    @TestConfiguration(proxyBeanMethods = false)
    static class RedisContainerConfig {
        @Bean
        @ServiceConnection(name = "redis")
        GenericContainer<?> redisContainer() {
            return new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine")).withExposedPorts(6379);
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    // 테스트에서 실제 메일을 보내지 않고, 보내려던 본문에서 인증번호만 꺼낸다
    @MockitoBean
    private EmailSender emailSender;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void correctCodeIsRejectedAfterFiveWrongAttempts() throws Exception {
        String email = "attempts@example.com";
        String code = sendCode(email);
        String wrongCode = code.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 4; i++) {
            confirm(email, wrongCode).andExpect(status().isBadRequest());
        }
        confirm(email, wrongCode).andExpect(status().isTooManyRequests());

        confirm(email, code).andExpect(status().isBadRequest());
    }

    @Test
    void verificationTokenCannotBeUsedTwice() throws Exception {
        String email = "reuse@example.com";
        String token = verifyEmail(email);

        signUp(email, token).andExpect(status().isCreated());

        // 중복 가입 검사(409)에 먼저 걸리지 않도록 계정을 지우고 토큰만 다시 검사한다
        userRepository.deleteAll();
        signUp(email, token).andExpect(status().isUnprocessableContent());
        assertThat(userRepository.existsByEmail(email)).isFalse();
    }

    @Test
    void verificationTokenCannotBeUsedForAnotherEmail() throws Exception {
        String token = verifyEmail("owner@example.com");

        signUp("other@example.com", token).andExpect(status().isUnprocessableContent());

        assertThat(userRepository.existsByEmail("other@example.com")).isFalse();
    }

    private String sendCode(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/email-verifications")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isNoContent());

        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(email), anyString(), text.capture());
        Matcher matcher = CODE_PATTERN.matcher(text.getValue());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private ResultActions confirm(String email, String code) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/email-verifications/confirm")
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}"));
    }

    private String verifyEmail(String email) throws Exception {
        String body = confirm(email, sendCode(email))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.verificationToken");
    }

    private ResultActions signUp(String email, String token) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/signup")
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"password123\",\"name\":\"테스터\",\"phoneNumber\":\"010-1234-5678\",\"defaultShippingAddress\":\"서울시 강남구 테헤란로 1\",\"birthDate\":\"1995-01-20\",\"gender\":\"MALE\",\"emailVerificationToken\":\"" + token + "\"}"));
    }
}
