package shop.daejung.daejungdistbackend.modules.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.daejung.daejungdistbackend.modules.user.domain.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
}
