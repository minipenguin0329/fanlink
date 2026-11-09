
package com.fanlink.domain.chat;

import com.fanlink.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void send(
            @Payload ChatSocketRequest request,
            Principal principal
    ) {
        // WebSocket에서 인증된 사용자 확인
        if (!(principal instanceof Authentication auth)
                || !(auth.getPrincipal() instanceof UserPrincipal user)) {
            throw new AccessDeniedException(
                    "로그인이 필요합니다."
            );
        }

        if (request == null || request.roomId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "채팅방 ID가 필요합니다."
            );
        }

        // 참가자 검사 + DB 메시지 저장
        ChatService.ChatDelivery delivery =
                chatService.sendForWebSocket(
                        request.roomId(),
                        user.getUserId(),
                        request.content()
                );

        ChatMessageResponse response = delivery.message();

        // 게시글 작성자에게 전송
        messagingTemplate.convertAndSendToUser(
                delivery.hostId().toString(),
                "/queue/chat",
                response
        );

        // 채팅에 참여한 상대방에게 전송
        if (!delivery.hostId().equals(delivery.participantId())) {
            messagingTemplate.convertAndSendToUser(
                    delivery.participantId().toString(),
                    "/queue/chat",
                    response
            );
        }
    }
}
