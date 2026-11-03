package shop.daejung.daejungdistbackend.modules.support.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import shop.daejung.daejungdistbackend.modules.support.domain.SupportPost;

import java.time.LocalDateTime;

public record SupportPostSummaryResponse(
        Long id,
        String title,
        String author,
        LocalDateTime createdAt,
        long views,
        @JsonProperty("isNotice") boolean isNotice
) {
    public static SupportPostSummaryResponse from(SupportPost post) {
        return new SupportPostSummaryResponse(
                post.getId(),
                post.getTitle(),
                post.getUser().getName(),
                post.getCreatedAt(),
                post.getViews(),
                post.isNotice()
        );
    }
}
