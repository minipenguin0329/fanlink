package com.fanlink.domain.chat;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import static org.mockito.Mockito.*;

class ChatMessageBroadcasterTest {
    @Test void sendsToBothUsers() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        ChatMessageBroadcaster broadcaster = new ChatMessageBroadcaster(template);
        ChatMessageResponse message = new ChatMessageResponse(11L, 7L, 2L, "hello", null);
        broadcaster.broadcast(new ChatService.ChatDelivery(message, 1L, 2L));
        verify(template).convertAndSendToUser("1", "/queue/chat", message);
        verify(template).convertAndSendToUser("2", "/queue/chat", message);
    }
}
