package shop.daejung.daejungdistbackend.modules.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshTokenCookieFactory {
    private final boolean cookieSecure;

    public RefreshTokenCookieFactory(@Value("${app.cookie.secure}") boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public ResponseCookie create(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/api/v1/auth/refresh")
                .maxAge(Duration.ofDays(7))
                .build();
    }
}
