package shop.daejung.daejungdistbackend.modules.support.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.daejung.daejungdistbackend.modules.support.dto.CreateSupportPostRequest;
import shop.daejung.daejungdistbackend.modules.support.dto.SupportPostDetailResponse;
import shop.daejung.daejungdistbackend.modules.support.service.SupportService;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/admin/supports")
@RequiredArgsConstructor
public class AdminSupportController {
    private final SupportService supportService;

    @PostMapping("/notices")
    public ResponseEntity<SupportPostDetailResponse> createNotice(
            Authentication authentication,
            @RequestBody @Valid CreateSupportPostRequest request
    ) {
        SupportPostDetailResponse response = supportService.createNotice(AuthenticatedEmail.from(authentication), request);
        return ResponseEntity.created(URI.create("/api/v1/supports/" + response.id())).body(response);
    }
}
