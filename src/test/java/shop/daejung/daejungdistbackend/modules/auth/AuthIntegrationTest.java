package shop.daejung.daejungdistbackend.modules.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import shop.daejung.daejungdistbackend.modules.auth.security.JwtTokenProvider;
import shop.daejung.daejungdistbackend.modules.user.domain.Role;
import shop.daejung.daejungdistbackend.modules.user.domain.User;
import shop.daejung.daejungdistbackend.modules.user.repository.UserRepository;

import static org.hamcrest.Matchers.containsString;
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

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=")));
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
}
