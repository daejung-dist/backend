package shop.daejung.daejungdistbackend.modules.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
}
