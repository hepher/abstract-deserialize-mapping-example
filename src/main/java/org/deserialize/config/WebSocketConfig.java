package org.deserialize.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // path prefix where client can subscribe notifications
        config.enableSimpleBroker("/topic");
        // topic/<nome_utente>/<tipo_notification>
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // path where the socket is opened ( point to register new SockJS() )
        registry.addEndpoint("/notificationbroker");
        registry.addEndpoint("/notificationbroker").withSockJS();
    }
}
