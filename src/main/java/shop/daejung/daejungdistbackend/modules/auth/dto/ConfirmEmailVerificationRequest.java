package shop.daejung.daejungdistbackend.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import shop.daejung.daejungdistbackend.global.util.EmailNormalizer;

public record ConfirmEmailVerificationRequest(
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @NotBlank(message = "이메일은 필수입니다.")
        String email,

        @NotBlank(message = "인증번호는 필수입니다.")
        @Pattern(regexp = "^[0-9]{6}$", message = "인증번호는 숫자 6자리입니다.")
        String code
) {
    public ConfirmEmailVerificationRequest {
        email = EmailNormalizer.normalize(email);
    }
}
