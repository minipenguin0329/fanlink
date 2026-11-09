
package com.fanlink.domain.chat;

import com.fanlink.domain.post.Post;
import com.fanlink.domain.user.User;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_rooms",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_post_participant",
                columnNames = {"post_id", "participant_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private User participant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomType type;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ChatRoom(
            Post post,
            User participant,
            ChatRoomType type
    ) {
        this.post = post;
        this.participant = participant;
        this.type = type;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isMember(Long userId) {
        if (userId == null) {
            return false;
        }

        return post.getAuthor().getId().equals(userId)
                || participant.getId().equals(userId);
    }
}
