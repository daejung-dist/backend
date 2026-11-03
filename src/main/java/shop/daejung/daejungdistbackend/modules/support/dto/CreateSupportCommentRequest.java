package shop.daejung.daejungdistbackend.modules.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import shop.daejung.daejungdistbackend.modules.support.domain.SupportComment;

public record CreateSupportCommentRequest(
        @NotBlank(message = "댓글 내용은 필수입니다.")
        @Size(max = SupportComment.CONTENT_MAX_LENGTH, message = "댓글은 1000자 이하여야 합니다.")
        String content
) {
}
