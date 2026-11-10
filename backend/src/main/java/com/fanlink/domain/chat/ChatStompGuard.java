
package com.fanlink.domain.chat;

import com.fanlink.domain.user.User;
import com.fanlink.domain.user.UserRepository;
import com.fanlink.security.JwtTokenProvider;
import com.fanlink.security.UserPrincipal;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatStompGuard implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (command == StompCommand.CONNECT) {
            authenticate(accessor);
        }

        if (command == StompCommand.SUBSCRIBE) {
            requireAuthenticated(accessor);

            if (!"/user/queue/chat".equals(
                    accessor.getDestination())) {
                throw new AccessDeniedException(
                        "허용되지 않은 채팅 구독 경로입니다."
                );
            }
        }

        if (command == StompCommand.SEND) {
            requireAuthenticated(accessor);

            if (!"/app/chat.send".equals(
                    accessor.getDestination())) {
                throw new AccessDeniedException(
                        "허용되지 않은 메시지 전송 경로입니다."
                );
            }
        }

        return message;
    }

    // STOMP CONNECT에서 JWT 인증
    private void authenticate(StompHeaderAccessor accessor) {

        String header =
                accessor.getFirstNativeHeader("Authorization");

        if (header == null ||
                !header.startsWith("Bearer ") ||
                header.substring(7).isBlank()) {
            throw new AccessDeniedException(
                    "WebSocket 연결에는 JWT가 필요합니다."
            );
        }

        String token = header.substring(7);

        try {
            Long userId = jwtTokenProvider.parseUserId(token);

            User user = userRepository.findById(userId)
                    .orElseThrow(() ->
                            new AccessDeniedException(
                                    "존재하지 않는 사용자입니다."
                            )
                    );

            UserPrincipal principal = UserPrincipal.of(user);

            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.getAuthorities()
                    );

            accessor.setUser(authentication);

        } catch (JwtException | IllegalArgumentException ex) {
            throw new AccessDeniedException(
                    "유효하지 않거나 만료된 JWT입니다.",
                    ex
            );
        }
    }

    // SUBSCRIBE와 SEND에서 인증 상태 확인
    private void requireAuthenticated(
            StompHeaderAccessor accessor) {

        if (!(accessor.getUser() instanceof Authentication auth)
                || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof UserPrincipal)) {

            throw new AccessDeniedException(
                    "인증되지 않은 WebSocket 사용자입니다."
            );
        }
    }
}
