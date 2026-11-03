package shop.daejung.daejungdistbackend.modules.support.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import shop.daejung.daejungdistbackend.modules.support.domain.SupportPost;

import java.util.List;
import java.util.Optional;

public interface SupportPostRepository extends JpaRepository<SupportPost, Long> {
    @EntityGraph(attributePaths = "user")
    List<SupportPost> findAllByNoticeTrueOrderByIdDesc();

    @EntityGraph(attributePaths = "user")
    Page<SupportPost> findAllByNoticeFalse(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<SupportPost> findAllByNoticeFalseAndTitleContaining(String keyword, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<SupportPost> findAllByNoticeFalseAndUserNameContaining(String keyword, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    @Query("""
            select p from SupportPost p
            where p.notice = false
              and (p.title like concat('%', :keyword, '%') or p.user.name like concat('%', :keyword, '%'))
            """)
    Page<SupportPost> searchByTitleOrAuthor(@Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    @Query("select p from SupportPost p where p.id = :id")
    Optional<SupportPost> findWithUserById(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SupportPost p set p.views = p.views + 1 where p.id = :id")
    int increaseViews(@Param("id") Long id);
}
