
package com.fanlink.domain.chat;

import com.fanlink.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;
    private final ChatMessageBroadcaster chatMessageBroadcaster;

    // 현재 로그인한 사용자 ID
    private Long currentUserId(UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        return principal.getUserId();
    }

    // 채팅방 생성
    @PostMapping("/posts/{postId}/chat-rooms")
    public ChatRoomResponse createRoom(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return chatService.createRoom(
                postId,
                currentUserId(principal)
        );
    }

    // 내 채팅방 목록
    @GetMapping("/chat/rooms")
    public List<ChatRoomResponse> getMyRooms(
            @AuthenticationPrincipal UserPrincipal principal) {

        return chatService.getMyRooms(
                currentUserId(principal)
        );
    }

    // 메시지 전송
    @PostMapping("/chat/rooms/{roomId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse sendMessage(
            @PathVariable Long roomId,
            @RequestBody ChatMessageRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "메시지 내용이 필요합니다.");
        }
        // Proxy transaction commits before we notify WebSocket listeners.
        ChatService.ChatDelivery delivery = chatService.sendForWebSocket(
                roomId, currentUserId(principal), request.content());
        chatMessageBroadcaster.broadcast(delivery);
        return delivery.message();
    }

    // 최근 메시지 조회
    @GetMapping("/chat/rooms/{roomId}/messages")
    public List<ChatMessageResponse> getMessages(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return chatService.getMessages(
                roomId,
                currentUserId(principal)
        );
    }
}
