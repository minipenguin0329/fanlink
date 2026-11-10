
package com.fanlink.domain.chat;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long messageId,
        Long roomId,
        Long senderId,
        String content,
        LocalDateTime createdAt
) {
    public static ChatMessageResponse from(
            ChatMessage message
    ) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRoom().getId(),
                message.getSender().getId(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
