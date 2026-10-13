package shop.daejung.daejungdistbackend.modules.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.daejung.daejungdistbackend.modules.auth.dto.LoginRequest;
import shop.daejung.daejungdistbackend.modules.auth.dto.SignUpRequest;
import shop.daejung.daejungdistbackend.modules.auth.security.RefreshTokenCookieFactory;
import shop.daejung.daejungdistbackend.modules.auth.service.AuthService;
import shop.daejung.daejungdistbackend.modules.user.domain.User;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signUp(@RequestBody @Valid SignUpRequest request) {
        User user = authService.signUp(request); // 유저 생성
        String accessToken = authService.issueToken(user); // accessToken 생성
        String refreshToken = authService.issueRefreshToken(user); // refreshToken 생성
        ResponseCookie refreshCookie = refreshTokenCookieFactory.create(refreshToken);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "name", user.getName(),
                        "role", user.getRole().name(),
                        "accessToken", accessToken
                ));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody @Valid LoginRequest request) {
        User user = authService.login(request); // db에서 꺼내온 user
        String accessToken = authService.issueToken(user);
        String refreshToken = authService.issueRefreshToken(user);
        ResponseCookie refreshCookie = refreshTokenCookieFactory.create(refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "name", user.getName(),
                        "role", user.getRole().name(),
                        "accessToken", accessToken
                ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(
            @CookieValue("refreshToken") String refreshTokenCookie
    ) {
        User user = authService.refresh(refreshTokenCookie);
        String accessToken = authService.issueToken(user);
        String nextRefreshToken = authService.issueRefreshToken(user);
        ResponseCookie refreshCookie = refreshTokenCookieFactory.create(nextRefreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "name", user.getName(),
                        "role", user.getRole().name(),
                        "accessToken", accessToken
                ));
    }

}
