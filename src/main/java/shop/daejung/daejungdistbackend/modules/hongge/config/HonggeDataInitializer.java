package shop.daejung.daejungdistbackend.modules.hongge.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import shop.daejung.daejungdistbackend.modules.hongge.domain.Hongge;
import shop.daejung.daejungdistbackend.modules.hongge.repository.HonggeRepository;

@Configuration
public class HonggeDataInitializer {
    @Bean
    public CommandLineRunner initHongge(
            HonggeRepository honggeRepository,
            @Value("${app.assets-base-url}") String assetsBaseUrl
    ) {
        String normalizedAssetsBaseUrl = assetsBaseUrl.endsWith("/")
                ? assetsBaseUrl.substring(0, assetsBaseUrl.length() - 1)
                : assetsBaseUrl;

        return args -> {
            if (honggeRepository.count() == 0) {
                honggeRepository.save(Hongge.builder()
                        .name("대정 특급 홍게")
                        .price(75000L)
                        .quantity(10)
                        .description("살이 꽉 찬 프리미엄급 홍게로 선물용으로 좋은 구성입니다.")
                        .thumbnailUrl(normalizedAssetsBaseUrl + "/images/hongge/premium_crab.jpg")
                        .build());
                honggeRepository.save(Hongge.builder()
                        .name("대정 실속형 홍게")
                        .price(59000L)
                        .quantity(20)
                        .description("가성비 좋은 실속 구성으로 가정용 식사에 잘 어울립니다.")
                        .thumbnailUrl(normalizedAssetsBaseUrl + "/images/hongge/economic_crab.jpg")
                        .build());
            }
        };
    }
}
