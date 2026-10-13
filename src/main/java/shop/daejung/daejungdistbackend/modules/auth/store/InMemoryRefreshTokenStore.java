package shop.daejung.daejungdistbackend.modules.auth.store;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(name = "app.refresh.store", havingValue = "in-memory")
public class InMemoryRefreshTokenStore implements RefreshTokenStore {
    private final Map<String, StoredRefreshToken> store = new ConcurrentHashMap<>();

    @Override
    public void save(String tokenId, String email, long ttlMillis) {
        store.put(tokenId, new StoredRefreshToken(email, Instant.now().plusMillis(ttlMillis)));
    }

    @Override
    public Optional<String> findEmail(String tokenId) {
        StoredRefreshToken stored = store.get(tokenId);
        if (stored == null) {
            return Optional.empty();
        }
        if (stored.expiresAt().isBefore(Instant.now())) {
            store.remove(tokenId);
            return Optional.empty();
        }
        return Optional.of(stored.email());
    }

    @Override
    public void delete(String tokenId) {
        store.remove(tokenId);
    }

    private record StoredRefreshToken(String email, Instant expiresAt) {
    }
}
