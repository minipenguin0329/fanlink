
package com.fanlink.domain.chat;

import java.time.LocalDateTime;

public record ChatRoomResponse(
        Long roomId,
        Long postId,
        Long hostId,
        Long participantId,
        ChatRoomType type,
        LocalDateTime createdAt
) {
    public static ChatRoomResponse from(ChatRoom room) {
        return new ChatRoomResponse(
                room.getId(),
                room.getPost().getId(),
                room.getPost().getAuthor().getId(),
                room.getParticipant().getId(),
                room.getType(),
                room.getCreatedAt()
        );
    }
}
