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
import shop.daejung.daejungdistbackend.modules.user.domain.Role;
import shop.daejung.daejungdistbackend.modules.user.domain.User;
import shop.daejung.daejungdistbackend.modules.user.repository.UserRepository;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                        .content("{\"email\":\"user@example.com\",\"password\":\"password123\",\"name\":\"테스터\"}"))
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
                .role(Role.ADMIN)
                .build());

        String adminToken = jwtTokenProvider.generateAccessToken(adminUser);

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
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
