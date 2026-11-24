package shop.daejung.daejungdistbackend.modules.auth.store;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Service
@ConditionalOnProperty(name = "app.refresh.store", havingValue = "redis", matchIfMissing = true)
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {
    private static final String KEY_PREFIX = "refresh:";
    // 탈퇴 시 한 사용자의 refresh token을 한 번에 지우려고 이메일별 tokenId 목록을 따로 둔다
    private static final String USER_KEY_PREFIX = "refresh-user:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(String tokenId, String email, long ttlMillis) {
        Duration ttl = Duration.ofMillis(ttlMillis);
        redisTemplate.opsForValue().set(key(tokenId), email, ttl);
        redisTemplate.opsForSet().add(userKey(email), tokenId);
        redisTemplate.expire(userKey(email), ttl);
    }

    @Override
    public Optional<String> findEmail(String tokenId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key(tokenId)));
    }

    @Override
    public void delete(String tokenId) {
        String email = redisTemplate.opsForValue().getAndDelete(key(tokenId));
        if (email != null) {
            redisTemplate.opsForSet().remove(userKey(email), tokenId);
        }
    }

    @Override
    public void deleteAllByEmail(String email) {
        Set<String> tokenIds = redisTemplate.opsForSet().members(userKey(email));
        if (tokenIds != null && !tokenIds.isEmpty()) {
            redisTemplate.delete(tokenIds.stream().map(this::key).toList());
        }
        redisTemplate.delete(userKey(email));
    }

    private String key(String tokenId) {
        return KEY_PREFIX + tokenId;
    }

    private String userKey(String email) {
        return USER_KEY_PREFIX + email;
    }
}
