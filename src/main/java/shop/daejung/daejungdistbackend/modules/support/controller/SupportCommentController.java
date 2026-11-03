package shop.daejung.daejungdistbackend.modules.support.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.daejung.daejungdistbackend.modules.support.dto.CreateSupportCommentRequest;
import shop.daejung.daejungdistbackend.modules.support.dto.SupportCommentResponse;
import shop.daejung.daejungdistbackend.modules.support.service.SupportService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/supports/{postId}/comments")
@RequiredArgsConstructor
public class SupportCommentController {
    private final SupportService supportService;

    @GetMapping
    public List<SupportCommentResponse> getComments(@PathVariable Long postId) {
        return supportService.getComments(postId);
    }

    @PostMapping
    public ResponseEntity<SupportCommentResponse> createComment(
            Authentication authentication,
            @PathVariable Long postId,
            @RequestBody @Valid CreateSupportCommentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(supportService.createComment(AuthenticatedEmail.from(authentication), postId, request));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            Authentication authentication,
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        supportService.deleteComment(AuthenticatedEmail.from(authentication), postId, commentId);
        return ResponseEntity.noContent().build();
    }
}
