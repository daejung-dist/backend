package shop.daejung.daejungdistbackend.modules.support.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import shop.daejung.daejungdistbackend.modules.support.domain.SupportPost;

import java.time.LocalDateTime;

public record SupportPostDetailResponse(
        Long id,
        String title,
        String content,
        Long authorId,
        String author,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long views,
        @JsonProperty("isNotice") boolean isNotice,
        long commentCount
) {
    public static SupportPostDetailResponse of(SupportPost post, long commentCount) {
        return new SupportPostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getUser().getId(),
                post.getUser().getName(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getViews(),
                post.isNotice(),
                commentCount
        );
    }
}
