package shop.daejung.daejungdistbackend.modules.support.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import shop.daejung.daejungdistbackend.modules.support.domain.SupportSearchField;
import shop.daejung.daejungdistbackend.modules.support.dto.CreateSupportPostRequest;
import shop.daejung.daejungdistbackend.modules.support.dto.SupportPostDetailResponse;
import shop.daejung.daejungdistbackend.modules.support.dto.SupportPostPageResponse;
import shop.daejung.daejungdistbackend.modules.support.dto.SupportPostSummaryResponse;
import shop.daejung.daejungdistbackend.modules.support.dto.UpdateSupportPostRequest;
import shop.daejung.daejungdistbackend.modules.support.service.SupportService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/supports")
@RequiredArgsConstructor
public class SupportPostController {
    private final SupportService supportService;

    @GetMapping("/notices")
    public List<SupportPostSummaryResponse> getNotices() {
        return supportService.getNotices();
    }

    @GetMapping
    public SupportPostPageResponse getPosts(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
            @RequestParam(defaultValue = "all") String field,
            @RequestParam(required = false) String keyword
    ) {
        return supportService.getPosts(page, size, SupportSearchField.from(field), keyword);
    }

    @GetMapping("/{postId}")
    public SupportPostDetailResponse getPost(@PathVariable Long postId) {
        return supportService.getPost(postId);
    }

    @PostMapping
    public ResponseEntity<SupportPostDetailResponse> createPost(
            Authentication authentication,
            @RequestBody @Valid CreateSupportPostRequest request
    ) {
        SupportPostDetailResponse response = supportService.createPost(AuthenticatedEmail.from(authentication), request);
        return ResponseEntity.created(URI.create("/api/v1/supports/" + response.id())).body(response);
    }

    @PatchMapping("/{postId}")
    public SupportPostDetailResponse updatePost(
            Authentication authentication,
            @PathVariable Long postId,
            @RequestBody @Valid UpdateSupportPostRequest request
    ) {
        if (!request.hasAnyUpdateField()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정할 정보가 없습니다.");
        }
        return supportService.updatePost(AuthenticatedEmail.from(authentication), postId, request);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(Authentication authentication, @PathVariable Long postId) {
        supportService.deletePost(AuthenticatedEmail.from(authentication), postId);
        return ResponseEntity.noContent().build();
    }
}
