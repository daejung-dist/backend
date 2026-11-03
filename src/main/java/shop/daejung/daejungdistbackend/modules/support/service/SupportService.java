package shop.daejung.daejungdistbackend.modules.support.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import shop.daejung.daejungdistbackend.modules.support.domain.SupportComment;
import shop.daejung.daejungdistbackend.modules.support.domain.SupportPost;
import shop.daejung.daejungdistbackend.modules.support.domain.SupportSearchField;
import shop.daejung.daejungdistbackend.modules.support.dto.CreateSupportCommentRequest;
import shop.daejung.daejungdistbackend.modules.support.dto.CreateSupportPostRequest;
import shop.daejung.daejungdistbackend.modules.support.dto.SupportCommentResponse;
import shop.daejung.daejungdistbackend.modules.support.dto.SupportPostDetailResponse;
import shop.daejung.daejungdistbackend.modules.support.dto.SupportPostPageResponse;
import shop.daejung.daejungdistbackend.modules.support.dto.SupportPostSummaryResponse;
import shop.daejung.daejungdistbackend.modules.support.dto.UpdateSupportPostRequest;
import shop.daejung.daejungdistbackend.modules.support.repository.SupportCommentRepository;
import shop.daejung.daejungdistbackend.modules.support.repository.SupportPostRepository;
import shop.daejung.daejungdistbackend.modules.user.domain.Role;
import shop.daejung.daejungdistbackend.modules.user.domain.User;
import shop.daejung.daejungdistbackend.modules.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportService {
    private final SupportPostRepository postRepository;
    private final SupportCommentRepository commentRepository;
    private final UserRepository userRepository;

//    게시글 조회
    public List<SupportPostSummaryResponse> getNotices() {
        return postRepository.findAllByNoticeTrueOrderByIdDesc().stream()
                .map(SupportPostSummaryResponse::from)
                .toList();
    }

    // 공지는 목록에서 빼고 getNotices로 따로 내려준다
    public SupportPostPageResponse getPosts(int page, int size, SupportSearchField field, String keyword) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id"));
        String trimmedKeyword = keyword == null ? "" : keyword.trim();

        Page<SupportPost> posts;
        if (trimmedKeyword.isEmpty()) {
            posts = postRepository.findAllByNoticeFalse(pageable);
        } else {
            posts = switch (field) {
                case TITLE -> postRepository.findAllByNoticeFalseAndTitleContaining(trimmedKeyword, pageable);
                case AUTHOR -> postRepository.findAllByNoticeFalseAndUserNameContaining(trimmedKeyword, pageable);
                case ALL -> postRepository.searchByTitleOrAuthor(trimmedKeyword, pageable);
            };
        }
        return SupportPostPageResponse.from(posts.map(SupportPostSummaryResponse::from));
    }

    // 상세를 열 때마다 조회수를 1 올린다
    @Transactional
    public SupportPostDetailResponse getPost(Long postId) {
        if (postRepository.increaseViews(postId) == 0) {
            throw postNotFound();
        }
        SupportPost post = postRepository.findWithUserById(postId).orElseThrow(this::postNotFound);
        return SupportPostDetailResponse.of(post, commentRepository.countByPostId(postId));
    }

//    게시글 작성·수정·삭제
    @Transactional
    public SupportPostDetailResponse createPost(String email, CreateSupportPostRequest request) {
        return savePost(getUser(email), request, false);
    }

    @Transactional
    public SupportPostDetailResponse createNotice(String email, CreateSupportPostRequest request) {
        return savePost(getUser(email), request, true);
    }

    @Transactional
    public SupportPostDetailResponse updatePost(String email, Long postId, UpdateSupportPostRequest request) {
        User user = getUser(email);
        SupportPost post = getPostEntity(postId);
        checkCanModify(post.isWrittenBy(user), user);

        post.update(request.title(), request.content());
        postRepository.flush();
        return SupportPostDetailResponse.of(post, commentRepository.countByPostId(postId));
    }

    @Transactional
    public void deletePost(String email, Long postId) {
        User user = getUser(email);
        SupportPost post = getPostEntity(postId);
        checkCanModify(post.isWrittenBy(user), user);

        commentRepository.deleteAllByPostId(postId);
        postRepository.deleteById(postId);
    }

//    댓글
    public List<SupportCommentResponse> getComments(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw postNotFound();
        }
        return commentRepository.findAllByPostIdOrderByIdAsc(postId).stream()
                .map(SupportCommentResponse::from)
                .toList();
    }

    @Transactional
    public SupportCommentResponse createComment(String email, Long postId, CreateSupportCommentRequest request) {
        User user = getUser(email);
        SupportPost post = getPostEntity(postId);

        SupportComment comment = commentRepository.save(SupportComment.builder()
                .post(post)
                .user(user)
                .content(request.content())
                .build());
        return SupportCommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(String email, Long postId, Long commentId) {
        User user = getUser(email);
        SupportComment comment = commentRepository.findById(commentId)
                .filter(found -> found.getPost().getId().equals(postId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));
        checkCanModify(comment.isWrittenBy(user), user);

        commentRepository.delete(comment);
    }

    private SupportPostDetailResponse savePost(User user, CreateSupportPostRequest request, boolean notice) {
        SupportPost post = postRepository.save(SupportPost.builder()
                .user(user)
                .title(request.title())
                .content(request.content())
                .notice(notice)
                .build());
        return SupportPostDetailResponse.of(post, 0);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 사용자를 찾을 수 없습니다."));
    }

    private SupportPost getPostEntity(Long postId) {
        return postRepository.findWithUserById(postId).orElseThrow(this::postNotFound);
    }

    // 작성자 본인과 관리자만 수정·삭제할 수 있다
    private void checkCanModify(boolean isAuthor, User user) {
        if (!isAuthor && user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정하거나 삭제할 권한이 없습니다.");
        }
    }

    private ResponseStatusException postNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.");
    }
}
