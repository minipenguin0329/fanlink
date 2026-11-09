
package com.fanlink.config;

import com.fanlink.domain.chat.ChatStompGuard;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;

import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.net.URI;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class ChatWebSocketConfig
        implements WebSocketMessageBrokerConfigurer {

    private final ChatStompGuard chatStompGuard;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void registerStompEndpoints(
            StompEndpointRegistry registry) {

        URI uri = URI.create(frontendUrl);

        String allowedOrigin =
                uri.getScheme() + "://" + uri.getAuthority();

        registry.addEndpoint("/ws/chat")
                .setAllowedOrigins(allowedOrigin);
    }

    @Override
    public void configureMessageBroker(
            MessageBrokerRegistry registry) {

        registry.enableSimpleBroker("/queue");

        registry.setApplicationDestinationPrefixes("/app");

        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(
            ChannelRegistration registration) {

        registration.interceptors(chatStompGuard);
    }
}
