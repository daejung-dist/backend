package shop.daejung.daejungdistbackend.modules.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import shop.daejung.daejungdistbackend.modules.auth.security.RefreshTokenCookieFactory;
import shop.daejung.daejungdistbackend.modules.auth.service.AuthService;
import shop.daejung.daejungdistbackend.modules.user.dto.MyInfoResponse;
import shop.daejung.daejungdistbackend.modules.user.dto.UpdateMyInfoRequest;
import shop.daejung.daejungdistbackend.modules.user.dto.WithdrawRequest;
import shop.daejung.daejungdistbackend.modules.user.service.UserService;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final AuthService authService;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    @GetMapping("/me")
    public ResponseEntity<MyInfoResponse> getMyInfo(Authentication authentication) {
        String email = extractAuthenticatedEmail(authentication);
        return userService.findByEmail(email)
                .map(MyInfoResponse::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 사용자를 찾을 수 없습니다."));
    }

    @PatchMapping("/me")
    public ResponseEntity<MyInfoResponse> updateMyInfo(
            Authentication authentication,
            @RequestBody @Valid UpdateMyInfoRequest request
    ) {
        if (!request.hasAnyUpdateField()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정할 정보가 없습니다.");
        }

        String email = extractAuthenticatedEmail(authentication);
        return ResponseEntity.ok(MyInfoResponse.from(userService.updateMyInfo(email, request)));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(
            Authentication authentication,
            @RequestBody @Valid WithdrawRequest request
    ) {
        String email = extractAuthenticatedEmail(authentication);
        authService.withdraw(email, request.password());

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.clear().toString())
                .build();
    }

    private String extractAuthenticatedEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 정보가 없습니다.");
        }
        return authentication.getName();
    }
}


