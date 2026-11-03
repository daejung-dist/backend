package shop.daejung.daejungdistbackend.modules.support.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

final class AuthenticatedEmail {
    private AuthenticatedEmail() {
    }

    static String from(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 정보가 없습니다.");
        }
        return authentication.getName();
    }
}
