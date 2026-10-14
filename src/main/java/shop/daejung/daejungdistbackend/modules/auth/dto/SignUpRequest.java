package shop.daejung.daejungdistbackend.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import shop.daejung.daejungdistbackend.modules.user.domain.Gender;

import java.time.LocalDate;

public record SignUpRequest(
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @NotBlank(message = "이메일은 필수입니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
        String password,

        @NotBlank(message = "이름은 필수입니다.")
        String name,

        @NotBlank(message = "휴대폰 번호는 필수입니다.")
        @Pattern(regexp = "^[0-9\\-+() ]{8,20}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
        String phoneNumber,

        @NotBlank(message = "기본 배송지 주소는 필수입니다.")
        String defaultShippingAddress,

        @NotNull(message = "생년월일은 필수입니다.")
        @Past(message = "생년월일은 과거 날짜여야 합니다.")
        LocalDate birthDate,

        @NotNull(message = "성별은 필수입니다.")
        Gender gender
) {
}
