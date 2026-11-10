package com.fanlink.domain.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/** Fan-out after the chat service transaction has completed. */
@Component
@RequiredArgsConstructor
public class ChatMessageBroadcaster {
    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(ChatService.ChatDelivery delivery) {
        ChatMessageResponse message = delivery.message();
        messagingTemplate.convertAndSendToUser(
                delivery.hostId().toString(), "/queue/chat", message);
        if (!delivery.hostId().equals(delivery.participantId())) {
            messagingTemplate.convertAndSendToUser(
                    delivery.participantId().toString(), "/queue/chat", message);
        }
    }
}
