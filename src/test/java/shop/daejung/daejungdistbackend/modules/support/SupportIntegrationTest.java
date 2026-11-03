package shop.daejung.daejungdistbackend.modules.support;

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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class SupportIntegrationTest {
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

    private MockMvc mockMvc;
    private User writer;
    private User other;
    private User admin;

    @BeforeEach
    void setUp() {
        cleanUp();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        writer = saveUser("writer@example.com", "김철수", Role.USER);
        other = saveUser("other@example.com", "이영희", Role.USER);
        admin = saveUser("admin@example.com", "대정유통", Role.ADMIN);
    }

    // 다른 테스트의 userRepository.deleteAll()이 외래 키에 막히지 않도록 게시판 데이터를 먼저 지운다
    @AfterEach
    void cleanUp() {
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void anyoneCanReadPostsAndNoticesAreSeparated() throws Exception {
        savePost(admin, "배송 안내", true);
        savePost(writer, "배송 문의", false);
        savePost(other, "환불 문의", false);

        mockMvc.perform(get("/api/v1/supports/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("배송 안내"))
                .andExpect(jsonPath("$[0].author").value("대정유통"))
                .andExpect(jsonPath("$[0].isNotice").value(true));

        mockMvc.perform(get("/api/v1/supports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts", hasSize(2)))
                .andExpect(jsonPath("$.posts[0].title").value("환불 문의"))
                .andExpect(jsonPath("$.posts[0].author").value("이영희"))
                .andExpect(jsonPath("$.posts[0].isNotice").value(false))
                .andExpect(jsonPath("$.posts[0].content").doesNotExist())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void postsArePagedAndSearchable() throws Exception {
        for (int i = 1; i <= 12; i++) {
            savePost(i % 2 == 0 ? writer : other, "문의 " + i, false);
        }
        savePost(writer, "철수네 홍게", false);

        mockMvc.perform(get("/api/v1/supports").param("page", "2"))
                .andExpect(jsonPath("$.posts", hasSize(3)))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.totalElements").value(13))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/v1/supports").param("field", "title").param("keyword", "홍게"))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/supports").param("field", "author").param("keyword", "김철수"))
                .andExpect(jsonPath("$.totalElements").value(7));

        // 전체 검색은 제목(철수네 홍게)과 작성자(김철수)를 함께 찾는다
        mockMvc.perform(get("/api/v1/supports").param("field", "all").param("keyword", "철수"))
                .andExpect(jsonPath("$.totalElements").value(7));

        mockMvc.perform(get("/api/v1/supports").param("page", "0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/supports").param("field", "content").param("keyword", "홍게"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void readingPostIncreasesViews() throws Exception {
        SupportPost post = savePost(writer, "배송 문의", false);

        mockMvc.perform(get("/api/v1/supports/" + post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.views").value(1));

        mockMvc.perform(get("/api/v1/supports/" + post.getId()))
                .andExpect(jsonPath("$.title").value("배송 문의"))
                .andExpect(jsonPath("$.content").value("내용"))
                .andExpect(jsonPath("$.authorId").value(writer.getId()))
                .andExpect(jsonPath("$.author").value("김철수"))
                .andExpect(jsonPath("$.views").value(2))
                .andExpect(jsonPath("$.commentCount").value(0));

        mockMvc.perform(get("/api/v1/supports/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createPostRequiresLogin() throws Exception {
        String body = "{\"title\":\"배송 문의\",\"content\":\"언제 오나요?\"}";

        mockMvc.perform(post("/api/v1/supports").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/supports")
                        .header("Authorization", bearer(writer))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.author").value("김철수"))
                .andExpect(jsonPath("$.isNotice").value(false));
    }

    @Test
    void createPostValidatesLength() throws Exception {
        mockMvc.perform(post("/api/v1/supports")
                        .header("Authorization", bearer(writer))
                        .contentType("application/json")
                        .content("{\"title\":\" \",\"content\":\"내용\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/supports")
                        .header("Authorization", bearer(writer))
                        .contentType("application/json")
                        .content("{\"title\":\"" + "가".repeat(101) + "\",\"content\":\"내용\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyAdminCanCreateNotice() throws Exception {
        String body = "{\"title\":\"공지\",\"content\":\"내용\"}";

        mockMvc.perform(post("/api/v1/admin/supports/notices")
                        .header("Authorization", bearer(writer))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/supports/notices")
                        .header("Authorization", bearer(admin))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isNotice").value(true));
    }

    @Test
    void onlyAuthorOrAdminCanUpdatePost() throws Exception {
        SupportPost post = savePost(writer, "수정 전", false);
        String url = "/api/v1/supports/" + post.getId();

        mockMvc.perform(patch(url).contentType("application/json").content("{\"title\":\"수정\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch(url)
                        .header("Authorization", bearer(other))
                        .contentType("application/json")
                        .content("{\"title\":\"남이 수정\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch(url)
                        .header("Authorization", bearer(writer))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch(url)
                        .header("Authorization", bearer(writer))
                        .contentType("application/json")
                        .content("{\"title\":\"수정 후\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정 후"))
                .andExpect(jsonPath("$.content").value("내용"));

        mockMvc.perform(patch(url)
                        .header("Authorization", bearer(admin))
                        .contentType("application/json")
                        .content("{\"content\":\"관리자 수정\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("관리자 수정"));
    }

    @Test
    void deletingPostRemovesItsComments() throws Exception {
        SupportPost post = savePost(writer, "삭제할 글", false);
        commentRepository.save(SupportComment.builder().post(post).user(admin).content("답변").build());
        String url = "/api/v1/supports/" + post.getId();

        mockMvc.perform(delete(url).header("Authorization", bearer(other)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(url).header("Authorization", bearer(writer)))
                .andExpect(status().isNoContent());

        assertThat(postRepository.existsById(post.getId())).isFalse();
        assertThat(commentRepository.count()).isZero();
    }

    @Test
    void commentsCanBeReadByAnyoneAndWrittenAfterLogin() throws Exception {
        SupportPost post = savePost(writer, "배송 문의", false);
        String url = "/api/v1/supports/" + post.getId() + "/comments";
        String body = "{\"content\":\"내일 출고됩니다.\"}";

        mockMvc.perform(post(url).contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(url)
                        .header("Authorization", bearer(admin))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author").value("대정유통"))
                .andExpect(jsonPath("$.content").value("내일 출고됩니다."));

        mockMvc.perform(post(url)
                        .header("Authorization", bearer(writer))
                        .contentType("application/json")
                        .content("{\"content\":\"" + "가".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].authorId").value(admin.getId()));

        mockMvc.perform(get("/api/v1/supports/" + post.getId()))
                .andExpect(jsonPath("$.commentCount").value(1));

        mockMvc.perform(get("/api/v1/supports/999999/comments"))
                .andExpect(status().isNotFound());
    }

    @Test
    void onlyAuthorOrAdminCanDeleteComment() throws Exception {
        SupportPost post = savePost(writer, "배송 문의", false);
        SupportPost otherPost = savePost(other, "다른 글", false);
        SupportComment comment = commentRepository.save(
                SupportComment.builder().post(post).user(writer).content("추가 문의").build());
        String url = "/api/v1/supports/" + post.getId() + "/comments/" + comment.getId();

        mockMvc.perform(delete(url).header("Authorization", bearer(other)))
                .andExpect(status().isForbidden());

        // 다른 게시글 경로로는 지울 수 없다
        mockMvc.perform(delete("/api/v1/supports/" + otherPost.getId() + "/comments/" + comment.getId())
                        .header("Authorization", bearer(writer)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete(url).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        assertThat(commentRepository.count()).isZero();
    }

    private User saveUser(String email, String name, Role role) {
        return userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .name(name)
                .phoneNumber("010-1234-5678")
                .defaultShippingAddress("서울시 강남구 테헤란로 1")
                .birthDate(LocalDate.of(1995, 1, 20))
                .gender(Gender.MALE)
                .role(role)
                .build());
    }

    private SupportPost savePost(User user, String title, boolean notice) {
        return postRepository.save(SupportPost.builder()
                .user(user)
                .title(title)
                .content("내용")
                .notice(notice)
                .build());
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(user);
    }
}
