package shop.daejung.daejungdistbackend.modules.support.dto;

import shop.daejung.daejungdistbackend.modules.support.domain.SupportComment;

import java.time.LocalDateTime;

public record SupportCommentResponse(
        Long id,
        String content,
        Long authorId,
        String author,
        LocalDateTime createdAt
) {
    public static SupportCommentResponse from(SupportComment comment) {
        return new SupportCommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getUser().getId(),
                comment.getUser().getName(),
                comment.getCreatedAt()
        );
    }
}
