package com.fanlink.domain.matching;

import com.fanlink.domain.post.Post;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

/** Serializes accept() calls for the same recruitment post. */
public interface MatchingPostLockRepository extends Repository<Post, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p where p.id = :postId")
    Optional<Post> findByIdForUpdate(@Param("postId") Long postId);
}
