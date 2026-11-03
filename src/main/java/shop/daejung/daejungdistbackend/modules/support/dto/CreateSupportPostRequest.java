package shop.daejung.daejungdistbackend.modules.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import shop.daejung.daejungdistbackend.modules.support.domain.SupportPost;

public record CreateSupportPostRequest(
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = SupportPost.TITLE_MAX_LENGTH, message = "제목은 100자 이하여야 합니다.")
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        @Size(max = SupportPost.CONTENT_MAX_LENGTH, message = "내용은 3000자 이하여야 합니다.")
        String content
) {
}
