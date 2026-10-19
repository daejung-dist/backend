package shop.daejung.daejungdistbackend.modules.user.dto;

import shop.daejung.daejungdistbackend.modules.user.domain.User;

import java.time.LocalDate;

public record MyInfoResponse(
        Long id,
        String email,
        String name,
        String role,
        String phoneNumber,
        String defaultShippingAddress,
        LocalDate birthDate,
        String gender
) {
    public static MyInfoResponse from(User user) {
        return new MyInfoResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.getPhoneNumber(),
                user.getDefaultShippingAddress(),
                user.getBirthDate(),
                user.getGender().name()
        );
    }
}

