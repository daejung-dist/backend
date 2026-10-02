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
                        .name("대정 특급 홍게")
                        .price(75000L)
                        .quantity(10)
                        .build());
                honggeRepository.save(Hongge.builder()
                        .name("대정 실속형 홍게")
                        .price(59000L)
                        .quantity(20)
                        .build());
            }
        };
    }
}
