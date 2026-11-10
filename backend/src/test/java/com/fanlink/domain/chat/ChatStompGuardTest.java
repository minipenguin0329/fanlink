
package com.fanlink.domain.chat;

import com.fanlink.domain.user.Role;
import com.fanlink.domain.user.User;
import com.fanlink.domain.user.UserRepository;
import com.fanlink.security.JwtTokenProvider;
import com.fanlink.security.UserPrincipal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatStompGuardTest {

    @Mock
    JwtTokenProvider jwtTokenProvider;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    ChatStompGuard guard;

    // 테스트용 STOMP 메시지 생성
    private Message<byte[]> frame(
            StompCommand command,
            String destination,
            Principal principal,
            String token) {

        StompHeaderAccessor accessor =
                StompHeaderAccessor.create(command);

        accessor.setSessionId("test-session");

        if (destination != null) {
            accessor.setDestination(destination);
        }

        if (principal != null) {
            accessor.setUser(principal);
        }

        if (token != null) {
            accessor.addNativeHeader(
                    "Authorization", "Bearer " + token
            );
        }

        accessor.setLeaveMutable(true);

        return MessageBuilder.createMessage(
                new byte[0],
                accessor.getMessageHeaders()
        );
    }

    @Test
    void connectWithoutTokenIsRejected() {

        Message<byte[]> message =
                frame(StompCommand.CONNECT, null, null, null);

        assertThrows(
                AccessDeniedException.class,
                () -> guard.preSend(
                        message, mock(MessageChannel.class)
                )
        );
    }

    @Test
    void validTokenRegistersUser() {

        User user = mock(User.class);

        when(jwtTokenProvider.parseUserId("test-token"))
                .thenReturn(5L);

        when(userRepository.findById(5L))
                .thenReturn(Optional.of(user));

        when(user.getId()).thenReturn(5L);
        when(user.getRole()).thenReturn(Role.USER);

        Message<byte[]> message =
                frame(
                        StompCommand.CONNECT,
                        null,
                        null,
                        "test-token"
                );

        guard.preSend(
                message, mock(MessageChannel.class)
        );

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );

        assertNotNull(accessor);
        assertNotNull(accessor.getUser());
        assertEquals("5", accessor.getUser().getName());
    }

    @Test
    void directQueueSubscriptionIsRejected() {

        UserPrincipal principal =
                new UserPrincipal(5L, Role.USER);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        Message<byte[]> message =
                frame(
                        StompCommand.SUBSCRIBE,
                        "/queue/chat",
                        authentication,
                        null
                );

        assertThrows(
                AccessDeniedException.class,
                () -> guard.preSend(
                        message, mock(MessageChannel.class)
                )
        );
    }

    @Test
    void authenticatedPersonalSubscriptionIsAllowed() {

        UserPrincipal principal =
                new UserPrincipal(5L, Role.USER);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        Message<byte[]> message =
                frame(
                        StompCommand.SUBSCRIBE,
                        "/user/queue/chat",
                        authentication,
                        null
                );

        assertDoesNotThrow(
                () -> guard.preSend(
                        message, mock(MessageChannel.class)
                )
        );
    }
}
