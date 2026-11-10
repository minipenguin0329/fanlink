
package com.fanlink.domain.chat;

import com.fanlink.domain.matching.MatchingRequestRepository;
import com.fanlink.domain.post.PostRepository;
import com.fanlink.domain.user.User;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    ChatRoomRepository roomRepository;

    @Mock
    ChatMessageRepository messageRepository;

    @Mock
    PostRepository postRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    MatchingRequestRepository matchingRepository;

    @InjectMocks
    ChatService chatService;

    @Test
    void outsiderCannotReadMessages() {

        ChatRoom room = mock(ChatRoom.class);

        when(roomRepository.findById(7L))
                .thenReturn(Optional.of(room));

        when(room.isMember(999L))
                .thenReturn(false);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> chatService.getMessages(7L, 999L)
                );

        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode()
        );

        verifyNoInteractions(messageRepository);
    }

    @Test
    void memberCanSendMessage() {

        ChatRoom room = mock(ChatRoom.class);
        User sender = mock(User.class);

        when(roomRepository.findById(7L))
                .thenReturn(Optional.of(room));

        when(room.isMember(3L))
                .thenReturn(true);

        when(room.getType())
                .thenReturn(ChatRoomType.TRADE);

        when(room.getId()).thenReturn(7L);
        when(sender.getId()).thenReturn(3L);

        when(userRepository.findById(3L))
                .thenReturn(Optional.of(sender));

        when(messageRepository.saveAndFlush(
                any(ChatMessage.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        ChatMessageResponse response =
                chatService.sendMessage(
                        7L,
                        3L,
                        "안녕하세요!"
                );

        assertEquals("안녕하세요!", response.content());
        assertEquals(7L, response.roomId());
        assertEquals(3L, response.senderId());

        verify(messageRepository).saveAndFlush(
                any(ChatMessage.class)
        );
    }
}
