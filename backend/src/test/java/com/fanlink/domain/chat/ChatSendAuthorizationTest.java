package com.fanlink.domain.chat;

import com.fanlink.domain.matching.MatchingRequestRepository;
import com.fanlink.domain.post.PostRepository;
import com.fanlink.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatSendAuthorizationTest {

    @Mock ChatRoomRepository roomRepository;
    @Mock ChatMessageRepository messageRepository;
    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;
    @Mock MatchingRequestRepository matchingRepository;
    @InjectMocks ChatService chatService;

    @Test
    void outsiderCannotSendMessage() {
        ChatRoom room = mock(ChatRoom.class);
        when(roomRepository.findById(7L)).thenReturn(Optional.of(room));
        when(room.isMember(999L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> chatService.sendMessage(7L, 999L, "접근 금지")
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(messageRepository);
    }
}
