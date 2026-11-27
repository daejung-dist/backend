package shop.daejung.daejungdistbackend.modules.hongge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import shop.daejung.daejungdistbackend.modules.hongge.domain.Hongge;

import java.util.List;

public interface HonggeRepository extends JpaRepository<Hongge, Long> {
    List<Hongge> findAllByOrderByIdAsc();

    // 조회 후 빼면 동시 주문이 같은 재고를 읽어 초과 판매되므로, 남은 재고 확인과 차감을 한 쿼리로 한다
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Hongge h set h.quantity = h.quantity - :amount where h.id = :id and h.quantity >= :amount")
    int decreaseQuantity(@Param("id") Long id, @Param("amount") int amount);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Hongge h set h.quantity = h.quantity + :amount where h.id = :id")
    int increaseQuantity(@Param("id") Long id, @Param("amount") int amount);
}
