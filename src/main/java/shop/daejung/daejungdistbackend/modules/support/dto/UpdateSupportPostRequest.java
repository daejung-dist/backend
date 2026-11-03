package shop.daejung.daejungdistbackend.modules.support.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import shop.daejung.daejungdistbackend.modules.support.domain.SupportPost;

public record UpdateSupportPostRequest(
        @Pattern(regexp = "(?s).*\\S.*", message = "제목은 비어 있을 수 없습니다.")
        @Size(max = SupportPost.TITLE_MAX_LENGTH, message = "제목은 100자 이하여야 합니다.")
        String title,

        @Pattern(regexp = "(?s).*\\S.*", message = "내용은 비어 있을 수 없습니다.")
        @Size(max = SupportPost.CONTENT_MAX_LENGTH, message = "내용은 3000자 이하여야 합니다.")
        String content
) {
    public boolean hasAnyUpdateField() {
        return title != null || content != null;
    }
}
