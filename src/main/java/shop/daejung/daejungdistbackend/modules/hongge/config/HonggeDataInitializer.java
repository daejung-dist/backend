package shop.daejung.daejungdistbackend.modules.hongge.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import shop.daejung.daejungdistbackend.modules.hongge.domain.Hongge;
import shop.daejung.daejungdistbackend.modules.hongge.repository.HonggeRepository;

@Configuration
public class HonggeDataInitializer {
    @Bean
    public CommandLineRunner initHongge(HonggeRepository honggeRepository) {
        return args -> {
            if (honggeRepository.count() == 0) {
                honggeRepository.save(Hongge.builder()
                        .name("프리미엄 홍게")
                        .price(59000L)
                        .quantity(10)
                        .build());
            }
        };
    }
}
