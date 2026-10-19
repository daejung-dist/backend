package shop.daejung.daejungdistbackend.modules.user.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import shop.daejung.daejungdistbackend.modules.user.domain.Gender;

import java.time.LocalDate;

public record UpdateMyInfoRequest(
        String name,

        @Pattern(regexp = "^[0-9\\-+() ]{8,20}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
        String phoneNumber,

        String defaultShippingAddress,

        @Past(message = "생년월일은 과거 날짜여야 합니다.")
        LocalDate birthDate,

        Gender gender
) {
    public boolean hasAnyUpdateField() {
        return name != null
                || phoneNumber != null
                || defaultShippingAddress != null
                || birthDate != null
                || gender != null;
    }
}

