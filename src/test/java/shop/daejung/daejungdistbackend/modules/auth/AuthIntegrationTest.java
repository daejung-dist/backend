package shop.daejung.daejungdistbackend.modules.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import shop.daejung.daejungdistbackend.modules.auth.security.JwtTokenProvider;
import shop.daejung.daejungdistbackend.modules.auth.store.RefreshTokenStore;
import shop.daejung.daejungdistbackend.modules.user.domain.Gender;
import shop.daejung.daejungdistbackend.modules.user.domain.Role;
import shop.daejung.daejungdistbackend.modules.user.domain.User;
import shop.daejung.daejungdistbackend.modules.user.repository.UserRepository;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuthIntegrationTest {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RefreshTokenStore refreshTokenStore;

    @Value("${jwt.refresh-secret}")
    private String refreshSecret;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void signUpReturnsAccessTokenAndRefreshCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType("application/json")
                        .content("{\"email\":\"user@example.com\",\"password\":\"password123\",\"name\":\"테스터\",\"phoneNumber\":\"010-1234-5678\",\"defaultShippingAddress\":\"서울시 강남구 테헤란로 1\",\"birthDate\":\"1995-01-20\",\"gender\":\"MALE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=")));
    }

    @Test
    void loginReturnsAccessTokenAndRefreshCookie() throws Exception {
        userRepository.save(User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password123"))
                .name("테스터")
                .phoneNumber("010-1234-5678")
                .defaultShippingAddress("서울시 강남구 테헤란로 1")
                .birthDate(LocalDate.of(1995, 1, 20))
                .gender(Gender.MALE)
                .role(Role.USER)
                .build());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"user@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=")));
    }

    @Test
    void refreshTokenReturnsNewAccessTokenWhenCookieExists() throws Exception {
        User user = userRepository.save(User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password123"))
                .name("테스터")
                .phoneNumber("010-1234-5678")
                .defaultShippingAddress("서울시 강남구 테헤란로 1")
                .birthDate(LocalDate.of(1995, 1, 20))
                .gender(Gender.MALE)
                .role(Role.USER)
                .build());

        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        refreshTokenStore.save(jwtTokenProvider.getTokenIdFromRefreshToken(refreshToken), user.getEmail(), jwtTokenProvider.getRefreshExpirationMs());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=")));
    }

    @Test
    void refreshWithoutCookieReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenIsRejectedAfterReuse() throws Exception {
        User user = userRepository.save(User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password123"))
                .name("테스터")
                .phoneNumber("010-1234-5678")
                .defaultShippingAddress("서울시 강남구 테헤란로 1")
                .birthDate(LocalDate.of(1995, 1, 20))
                .gender(Gender.MALE)
                .role(Role.USER)
                .build());

        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        refreshTokenStore.save(jwtTokenProvider.getTokenIdFromRefreshToken(refreshToken), user.getEmail(), jwtTokenProvider.getRefreshExpirationMs());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenWithoutJtiIsRejected() throws Exception {
        User user = userRepository.save(User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password123"))
                .name("테스터")
                .phoneNumber("010-1234-5678")
                .defaultShippingAddress("서울시 강남구 테헤란로 1")
                .birthDate(LocalDate.of(1995, 1, 20))
                .gender(Gender.MALE)
                .role(Role.USER)
                .build());

        String refreshTokenWithoutJti = createRefreshTokenWithoutJti(user);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshTokenWithoutJti)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesRefreshTokenAndClearsCookie() throws Exception {
        User user = userRepository.save(User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password123"))
                .name("테스터")
                .phoneNumber("010-1234-5678")
                .defaultShippingAddress("서울시 강남구 테헤란로 1")
                .birthDate(LocalDate.of(1995, 1, 20))
                .gender(Gender.MALE)
                .role(Role.USER)
                .build());

        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        String tokenId = jwtTokenProvider.getTokenIdFromRefreshToken(refreshToken);
        refreshTokenStore.save(tokenId, user.getEmail(), jwtTokenProvider.getRefreshExpirationMs());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

        org.assertj.core.api.Assertions.assertThat(refreshTokenStore.findEmail(tokenId)).isEmpty();
    }

    @Test
    void logoutWithoutCookieStillClearsCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")))
                .andExpect(header().string("Set-Cookie", not(containsString("refreshToken=null"))));
    }

    @Test
    void logoutWithInvalidTokenStillClearsCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "invalid-token")))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }

    @Test
    void adminEndpointRequiresAdminRole() throws Exception {
        User normalUser = userRepository.save(User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("password123"))
                .name("일반유저")
                .phoneNumber("010-1234-5678")
                .defaultShippingAddress("서울시 강남구 테헤란로 1")
                .birthDate(LocalDate.of(1995, 1, 20))
                .gender(Gender.MALE)
                .role(Role.USER)
                .build());

        String userToken = jwtTokenProvider.generateAccessToken(normalUser);

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        User adminUser = userRepository.save(User.builder()
                .email("admin@example.com")
                .password(passwordEncoder.encode("password123"))
                .name("관리자")
                .phoneNumber("010-9999-9999")
                .defaultShippingAddress("서울시 서초구 반포대로 1")
                .birthDate(LocalDate.of(1990, 5, 5))
                .gender(Gender.FEMALE)
                .role(Role.ADMIN)
                .build());

        String adminToken = jwtTokenProvider.generateAccessToken(adminUser);

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void myInfoReturnsCurrentUserWhenAuthenticated() throws Exception {
        User user = userRepository.save(User.builder()
                .email("me@example.com")
                .password(passwordEncoder.encode("password123"))
                .name("내정보유저")
                .phoneNumber("010-1111-2222")
                .defaultShippingAddress("서울시 중구 세종대로 1")
                .birthDate(LocalDate.of(1994, 6, 15))
                .gender(Gender.FEMALE)
                .role(Role.USER)
                .build());

        String accessToken = jwtTokenProvider.generateAccessToken(user);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.name").value("내정보유저"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.phoneNumber").value("010-1111-2222"))
                .andExpect(jsonPath("$.defaultShippingAddress").value("서울시 중구 세종대로 1"))
                .andExpect(jsonPath("$.birthDate").value("1994-06-15"))
                .andExpect(jsonPath("$.gender").value("FEMALE"));
    }

    @Test
    void myInfoRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateMyInfoUpdatesCurrentUserWhenAuthenticated() throws Exception {
        User user = userRepository.save(User.builder()
                .email("patch@example.com")
                .password(passwordEncoder.encode("password123"))
                .name("수정전")
                .phoneNumber("010-0000-0000")
                .defaultShippingAddress("서울시 강북구 초기주소 1")
                .birthDate(LocalDate.of(1992, 2, 2))
                .gender(Gender.OTHER)
                .role(Role.USER)
                .build());

        String accessToken = jwtTokenProvider.generateAccessToken(user);

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("{\"name\":\"수정후\",\"phoneNumber\":\"010-3333-4444\",\"defaultShippingAddress\":\"서울시 마포구 새주소 100\",\"birthDate\":\"1991-01-01\",\"gender\":\"MALE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("patch@example.com"))
                .andExpect(jsonPath("$.name").value("수정후"))
                .andExpect(jsonPath("$.phoneNumber").value("010-3333-4444"))
                .andExpect(jsonPath("$.defaultShippingAddress").value("서울시 마포구 새주소 100"))
                .andExpect(jsonPath("$.birthDate").value("1991-01-01"))
                .andExpect(jsonPath("$.gender").value("MALE"));
    }

    @Test
    void updateMyInfoRequiresAuthentication() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType("application/json")
                        .content("{\"name\":\"수정후\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateMyInfoRejectsEmptyBody() throws Exception {
        User user = userRepository.save(User.builder()
                .email("empty@example.com")
                .password(passwordEncoder.encode("password123"))
                .name("빈요청")
                .phoneNumber("010-1234-0000")
                .defaultShippingAddress("서울시 성동구 어딘가 2")
                .birthDate(LocalDate.of(1990, 10, 10))
                .gender(Gender.FEMALE)
                .role(Role.USER)
                .build());

        String accessToken = jwtTokenProvider.generateAccessToken(user);

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private String createRefreshTokenWithoutJti(User user) {
        SecretKey key = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtTokenProvider.getRefreshExpirationMs());

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .claim("tokenType", "REFRESH")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }
}
