package shop.daejung.daejungdistbackend.modules.auth.store;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@ConditionalOnProperty(name = "app.refresh.store", havingValue = "redis", matchIfMissing = true)
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {
    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(String tokenId, String email, long ttlMillis) {
        redisTemplate.opsForValue().set(key(tokenId), email, Duration.ofMillis(ttlMillis));
    }

    @Override
    public Optional<String> findEmail(String tokenId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key(tokenId)));
    }

    @Override
    public void delete(String tokenId) {
        redisTemplate.delete(key(tokenId));
    }

    private String key(String tokenId) {
        return KEY_PREFIX + tokenId;
    }
}
