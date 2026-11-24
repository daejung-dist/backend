package shop.daejung.daejungdistbackend.modules.user;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import shop.daejung.daejungdistbackend.modules.auth.security.JwtTokenProvider;
import shop.daejung.daejungdistbackend.modules.auth.store.RefreshTokenStore;
import shop.daejung.daejungdistbackend.modules.support.domain.SupportComment;
import shop.daejung.daejungdistbackend.modules.support.domain.SupportPost;
import shop.daejung.daejungdistbackend.modules.support.repository.SupportCommentRepository;
import shop.daejung.daejungdistbackend.modules.support.repository.SupportPostRepository;
import shop.daejung.daejungdistbackend.modules.user.domain.Gender;
import shop.daejung.daejungdistbackend.modules.user.domain.Role;
import shop.daejung.daejungdistbackend.modules.user.domain.User;
import shop.daejung.daejungdistbackend.modules.user.repository.UserRepository;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class WithdrawIntegrationTest {
    private static final String PASSWORD = "password123";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SupportPostRepository postRepository;

    @Autowired
    private SupportCommentRepository commentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RefreshTokenStore refreshTokenStore;

    private MockMvc mockMvc;
    private User user;
    private User other;

    @BeforeEach
    void setUp() {
        cleanUp();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        user = saveUser("bye@example.com", "탈퇴회원");
        other = saveUser("stay@example.com", "남는회원");
    }

    @AfterEach
    void cleanUp() {
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void withdrawDeletesUserAndWrittenSupportData() throws Exception {
        SupportPost myPost = savePost(user);
        saveComment(myPost, other);
        SupportPost otherPost = savePost(other);
        saveComment(otherPost, user);
        SupportComment otherComment = saveComment(otherPost, other);

        String refreshTokenId = saveRefreshToken(user);

        mockMvc.perform(withdrawRequest(user, PASSWORD))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

        assertThat(userRepository.findById(user.getId())).isEmpty();
        assertThat(userRepository.findById(other.getId())).isPresent();
        assertThat(postRepository.findAll()).extracting(SupportPost::getId).containsExactly(otherPost.getId());
        assertThat(commentRepository.findAll()).extracting(SupportComment::getId).containsExactly(otherComment.getId());
        assertThat(refreshTokenStore.findEmail(refreshTokenId)).isEmpty();
    }

    @Test
    void withdrawRejectsWrongPassword() throws Exception {
        mockMvc.perform(withdrawRequest(user, "wrong-password"))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.findById(user.getId())).isPresent();
    }

    @Test
    void withdrawRejectsMissingPassword() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", bearer(user))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.findById(user.getId())).isPresent();
    }

    @Test
    void withdrawRequiresAuthentication() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me")
                        .contentType("application/json")
                        .content("{\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());

        assertThat(userRepository.findById(user.getId())).isPresent();
    }

    @Test
    void tokensIssuedBeforeWithdrawDoNotWorkForReSignedUpAccount() throws Exception {
        String oldAccessToken = bearer(user);
        String oldRefreshToken = jwtTokenProvider.generateRefreshToken(user);
        refreshTokenStore.save(jwtTokenProvider.getTokenIdFromRefreshToken(oldRefreshToken), user.getEmail(),
                jwtTokenProvider.getRefreshExpirationMs());

        mockMvc.perform(withdrawRequest(user, PASSWORD))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", oldAccessToken))
                .andExpect(status().isUnauthorized());

        saveUser("bye@example.com", "재가입회원");

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", oldAccessToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refreshToken", oldRefreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenIsRejectedWhenUserIdDoesNotMatch() throws Exception {
        String oldRefreshToken = jwtTokenProvider.generateRefreshToken(user);
        // 저장소 정리가 실패해 토큰이 남아 있어도 재가입 계정으로 재발급되면 안 된다
        String tokenId = jwtTokenProvider.getTokenIdFromRefreshToken(oldRefreshToken);

        userRepository.deleteById(user.getId());
        saveUser("bye@example.com", "재가입회원");
        refreshTokenStore.save(tokenId, "bye@example.com", jwtTokenProvider.getRefreshExpirationMs());

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refreshToken", oldRefreshToken)))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.RequestBuilder withdrawRequest(User target, String password) {
        return delete("/api/v1/users/me")
                .header("Authorization", bearer(target))
                .contentType("application/json")
                .content("{\"password\":\"" + password + "\"}");
    }

    private String saveRefreshToken(User target) {
        String refreshToken = jwtTokenProvider.generateRefreshToken(target);
        String tokenId = jwtTokenProvider.getTokenIdFromRefreshToken(refreshToken);
        refreshTokenStore.save(tokenId, target.getEmail(), jwtTokenProvider.getRefreshExpirationMs());
        return tokenId;
    }

    private User saveUser(String email, String name) {
        return userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .name(name)
                .phoneNumber("010-1234-5678")
                .defaultShippingAddress("서울시 강남구 테헤란로 1")
                .birthDate(LocalDate.of(1995, 1, 20))
                .gender(Gender.MALE)
                .role(Role.USER)
                .build());
    }

    private SupportPost savePost(User writer) {
        return postRepository.save(SupportPost.builder()
                .user(writer)
                .title("문의")
                .content("내용")
                .notice(false)
                .build());
    }

    private SupportComment saveComment(SupportPost post, User writer) {
        return commentRepository.save(SupportComment.builder()
                .post(post)
                .user(writer)
                .content("댓글")
                .build());
    }

    private String bearer(User target) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(target);
    }
}
