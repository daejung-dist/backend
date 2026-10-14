package shop.daejung.daejungdistbackend.modules.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.daejung.daejungdistbackend.modules.auth.dto.SignUpRequest;
import shop.daejung.daejungdistbackend.modules.user.domain.Role;
import shop.daejung.daejungdistbackend.modules.user.domain.User;
import shop.daejung.daejungdistbackend.modules.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

//    사용자 생성
    @Transactional
    public User createUser(SignUpRequest request) {
        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.builder()
                .email(request.email())
                .password(encodedPassword)
                .name(request.name())
                .role(Role.USER)
                .phoneNumber(request.phoneNumber())
                .defaultShippingAddress(request.defaultShippingAddress())
                .birthDate(request.birthDate())
                .gender(request.gender())
                .build();

        return userRepository.save(user);
    }

//    사용자 조회
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }
}
