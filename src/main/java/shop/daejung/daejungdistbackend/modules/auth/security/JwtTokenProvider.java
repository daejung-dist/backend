package shop.daejung.daejungdistbackend.modules.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import shop.daejung.daejungdistbackend.modules.user.domain.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {
    private final SecretKey accessKey;
    private final SecretKey refreshKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

//    JwtTokenProvider를 bean으로 만들때 secret키 읽어오고 검증 및 hmac 변환
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.refresh-secret}") String refreshSecret,
            @Value("${jwt.expiration-ms}") long accessExpirationMs,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT access secret must be configured.");
        }
        if (refreshSecret == null || refreshSecret.isBlank()) {
            throw new IllegalStateException("JWT refresh secret must be configured.");
        }

        this.accessKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    // 토큰 생성
    public String generateAccessToken(User user) {
        return generateToken(user, "ACCESS", accessExpirationMs, accessKey);
    }

    public String generateRefreshToken(User user) {
        String tokenId = UUID.randomUUID().toString();
        return generateToken(user, "REFRESH", refreshExpirationMs, refreshKey, tokenId);
    }

    private String generateToken(User user, String tokenType, long expirationMs, SecretKey key) {
        return generateToken(user, tokenType, expirationMs, key, null);
    }

    private String generateToken(User user, String tokenType, long expirationMs, SecretKey key, String tokenId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        var builder = Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .claim("tokenType", tokenType)
                .issuedAt(now)
                .expiration(expiration);

        if (tokenId != null && !tokenId.isBlank()) {
            builder.id(tokenId);
        }

        return builder.signWith(key).compact();
    }

    // 토큰 검증
    public boolean validateAccessToken(String token) {
        try {
            validateAndGetClaims(token, accessKey);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            validateAndGetClaims(token, refreshKey);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims validateAndGetAccessClaims(String token) {
        return validateAndGetClaims(token, accessKey);
    }

    public Claims validateAndGetRefreshClaims(String token) {
        return validateAndGetClaims(token, refreshKey);
    }

    public Claims validateAndGetClaims(String token, SecretKey key) {
        try {
            return parseClaims(token, key);
        } catch (Exception e) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.", e);
        }
    }

    public String getEmailFromAccessToken(String token) {
        return validateAndGetAccessClaims(token).getSubject();
    }

    public String getEmailFromRefreshToken(String token) {
        return validateAndGetRefreshClaims(token).getSubject();
    }

    public String getTokenIdFromRefreshToken(String token) {
        String tokenId = validateAndGetRefreshClaims(token).getId();
        if (tokenId == null || tokenId.isBlank()) {
            throw new IllegalArgumentException("Refresh token ID is missing.");
        }
        return tokenId;
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    private Claims parseClaims(String token, SecretKey key) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


}
