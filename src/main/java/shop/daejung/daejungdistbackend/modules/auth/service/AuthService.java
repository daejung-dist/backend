package shop.daejung.daejungdistbackend.modules.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import shop.daejung.daejungdistbackend.modules.auth.dto.LoginRequest;
import shop.daejung.daejungdistbackend.modules.auth.dto.SignUpRequest;
import shop.daejung.daejungdistbackend.modules.auth.security.JwtTokenProvider;
import shop.daejung.daejungdistbackend.modules.auth.store.RefreshTokenStore;
import shop.daejung.daejungdistbackend.modules.user.domain.User;
import shop.daejung.daejungdistbackend.modules.user.service.UserService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private static final String INVALID_REFRESH_TOKEN_MESSAGE = "유효하지 않은 refresh token입니다.";

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User signUp(SignUpRequest request) {
        // 중복 유저 검증
        if (userService.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
        }
        // 유저 생성
        return userService.createUser(request);
    }

    public User login(LoginRequest request) {
        User user = userService.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));

        // 비밀번호 대조
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return user;
    }

    public String issueToken(User user) {
        return jwtTokenProvider.generateAccessToken(user);
    }

    public String issueRefreshToken(User user) {
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        String tokenId = jwtTokenProvider.getTokenIdFromRefreshToken(refreshToken);
        refreshTokenStore.save(tokenId, user.getEmail(), jwtTokenProvider.getRefreshExpirationMs());
        return refreshToken;
    }

    public User refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw invalidRefreshToken();
        }

        try {
            String email = jwtTokenProvider.validateAndGetRefreshClaims(refreshToken).getSubject();
            String tokenId = jwtTokenProvider.getTokenIdFromRefreshToken(refreshToken);

            String storedEmail = refreshTokenStore.findEmail(tokenId)
                    .orElseThrow(this::invalidRefreshToken);

            if (!storedEmail.equals(email)) {
                throw invalidRefreshToken();
            }

            refreshTokenStore.delete(tokenId);

            return userService.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));
        } catch (IllegalArgumentException e) {
            throw invalidRefreshToken();
        }
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        try {
            String tokenId = jwtTokenProvider.getTokenIdFromRefreshToken(refreshToken);
            refreshTokenStore.delete(tokenId);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private ResponseStatusException invalidRefreshToken() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_REFRESH_TOKEN_MESSAGE);
    }
}
