package shop.daejung.daejungdistbackend.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import shop.daejung.daejungdistbackend.global.util.EmailNormalizer;

public record SendEmailVerificationRequest(
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @NotBlank(message = "이메일은 필수입니다.")
        String email
) {
    public SendEmailVerificationRequest {
        email = EmailNormalizer.normalize(email);
    }
}
