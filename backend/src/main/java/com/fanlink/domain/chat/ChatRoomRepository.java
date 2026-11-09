
package com.fanlink.domain.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository
        extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByPost_IdAndParticipant_Id(
            Long postId,
            Long participantId
    );

    @Query("""
        SELECT r FROM ChatRoom r
        WHERE r.post.author.id = :userId
           OR r.participant.id = :userId
        ORDER BY r.createdAt DESC, r.id DESC
        """)
    List<ChatRoom> findAllForUser(
            @Param("userId") Long userId
    );
}
