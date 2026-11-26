package shop.daejung.daejungdistbackend.modules.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.daejung.daejungdistbackend.modules.auth.dto.ConfirmEmailVerificationRequest;
import shop.daejung.daejungdistbackend.modules.auth.dto.EmailVerificationTokenResponse;
import shop.daejung.daejungdistbackend.modules.auth.dto.SendEmailVerificationRequest;
import shop.daejung.daejungdistbackend.modules.auth.service.EmailVerificationService;

@RestController
@RequestMapping("/api/v1/auth/email-verifications")
@RequiredArgsConstructor
public class EmailVerificationController {
    private final EmailVerificationService emailVerificationService;

    @PostMapping
    public ResponseEntity<Void> sendCode(@RequestBody @Valid SendEmailVerificationRequest request) {
        emailVerificationService.sendCode(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/confirm")
    public ResponseEntity<EmailVerificationTokenResponse> confirm(@RequestBody @Valid ConfirmEmailVerificationRequest request) {
        String token = emailVerificationService.confirm(request.email(), request.code());
        return ResponseEntity.ok(new EmailVerificationTokenResponse(token));
    }
}
