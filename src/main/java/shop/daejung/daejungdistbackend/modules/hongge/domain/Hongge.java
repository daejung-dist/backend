package shop.daejung.daejungdistbackend.modules.hongge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hongge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false)
    private Integer quantity;

    @Column
    private String description;

    @Column
    private String thumbnailUrl;

    @Builder
    private Hongge(String name, Long price, Integer quantity, String description, String thumbnailUrl) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
    }
}
