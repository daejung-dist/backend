package shop.daejung.daejungdistbackend.global.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import shop.daejung.daejungdistbackend.modules.auth.security.JwtTokenProvider;
import shop.daejung.daejungdistbackend.modules.user.domain.Gender;
import shop.daejung.daejungdistbackend.modules.user.domain.Role;
import shop.daejung.daejungdistbackend.modules.user.domain.User;
import shop.daejung.daejungdistbackend.modules.user.repository.UserRepository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

// MockMvc는 /error 디스패치를 거치지 않으므로 실제 서버를 띄워 상태 코드를 확인한다
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ErrorResponseIntegrationTest {
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void notFoundIsNotTurnedIntoForbidden() throws Exception {
        assertThat(send(HttpRequest.newBuilder(uri("/api/v1/supports/999999")).GET()).statusCode()).isEqualTo(404);
        assertThat(send(HttpRequest.newBuilder(uri("/api/v1/hongges/999999")).GET()).statusCode()).isEqualTo(404);
    }

    @Test
    void badRequestIsNotTurnedIntoForbidden() throws Exception {
        assertThat(send(HttpRequest.newBuilder(uri("/api/v1/supports?page=0")).GET()).statusCode()).isEqualTo(400);
    }

    @Test
    void authenticatedValidationErrorReturnsBadRequest() throws Exception {
        User user = userRepository.save(User.builder()
                .email("error@example.com")
                .password(passwordEncoder.encode("password123"))
                .name("오류확인")
                .phoneNumber("010-1234-5678")
                .defaultShippingAddress("서울시 강남구 테헤란로 1")
                .birthDate(LocalDate.of(1995, 1, 20))
                .gender(Gender.MALE)
                .role(Role.USER)
                .build());

        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/v1/supports"))
                .header("Authorization", "Bearer " + jwtTokenProvider.generateAccessToken(user))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"title\":\" \",\"content\":\"내용\"}")));

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void unauthenticatedWriteReturnsUnauthorized() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/v1/supports"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"title\":\"제목\",\"content\":\"내용\"}")));

        assertThat(response.statusCode()).isEqualTo(401);
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
