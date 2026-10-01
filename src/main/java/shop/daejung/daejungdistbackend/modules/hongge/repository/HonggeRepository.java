package shop.daejung.daejungdistbackend.modules.hongge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.daejung.daejungdistbackend.modules.hongge.domain.Hongge;

import java.util.List;

public interface HonggeRepository extends JpaRepository<Hongge, Long> {
    List<Hongge> findAllByOrderByIdAsc();
}
