package shop.daejung.daejungdistbackend.modules.support.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import shop.daejung.daejungdistbackend.modules.support.domain.SupportComment;

import java.util.List;

public interface SupportCommentRepository extends JpaRepository<SupportComment, Long> {
    @EntityGraph(attributePaths = "user")
    List<SupportComment> findAllByPostIdOrderByIdAsc(Long postId);

    long countByPostId(Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from SupportComment c where c.post.id = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);
}
