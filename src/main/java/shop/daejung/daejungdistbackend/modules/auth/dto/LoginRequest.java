package shop.daejung.daejungdistbackend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import shop.daejung.daejungdistbackend.global.util.EmailNormalizer;

public record LoginRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
    public LoginRequest {
        email = EmailNormalizer.normalize(email);
    }
}
