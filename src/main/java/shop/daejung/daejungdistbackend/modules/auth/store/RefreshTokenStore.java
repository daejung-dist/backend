package shop.daejung.daejungdistbackend.modules.auth.store;

import java.util.Optional;

public interface RefreshTokenStore {
    void save(String tokenId, String email, long ttlMillis);

    Optional<String> findEmail(String tokenId);

    void delete(String tokenId);
}
