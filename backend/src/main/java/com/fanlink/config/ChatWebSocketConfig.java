
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
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class ChatWebSocketConfig
        implements WebSocketMessageBrokerConfigurer {

    private final ChatStompGuard chatStompGuard;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // Optional explicit origins for local Expo or deployed React Native web.
    // Example: FANLINK_WS_ALLOWED_ORIGINS=http://192.168.10.7:8081
    @Value("${FANLINK_WS_ALLOWED_ORIGINS:}")
    private String additionalOrigins;

    @Override
    public void registerStompEndpoints(
            StompEndpointRegistry registry) {

        URI uri = URI.create(frontendUrl);

        String allowedOrigin =
                uri.getScheme() + "://" + uri.getAuthority();

        List<String> origins = new ArrayList<>();
        origins.add(allowedOrigin);
        if (additionalOrigins != null && !additionalOrigins.isBlank()) {
            for (String value : additionalOrigins.split(",")) {
                String origin = value.trim();
                if (!origin.isBlank() && !origin.equals("*")) {
                    origins.add(origin);
                }
            }
        }
        registry.addEndpoint("/ws/chat")
                .setAllowedOrigins(origins.toArray(String[]::new));
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
