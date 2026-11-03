package shop.daejung.daejungdistbackend.modules.support.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record SupportPostPageResponse(
        List<SupportPostSummaryResponse> posts,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static SupportPostPageResponse from(Page<SupportPostSummaryResponse> page) {
        return new SupportPostPageResponse(
                page.getContent(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
