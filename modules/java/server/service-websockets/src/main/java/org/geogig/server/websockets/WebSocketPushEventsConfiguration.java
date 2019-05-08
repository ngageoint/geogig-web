package org.geogig.server.websockets;

import java.util.Map;

import org.geogig.server.websockets.internal.ActivityEntry;
import org.geogig.server.websockets.internal.ActivityLogRepository;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import lombok.NonNull;

@Configuration
@EnableAutoConfiguration
@EnableWebSocketMessageBroker
@ComponentScan(basePackageClasses = WebSocketsRPCController.class, lazyInit = true)
@EntityScan(basePackageClasses = ActivityEntry.class)
@EnableJpaRepositories(basePackageClasses = ActivityLogRepository.class)
public class WebSocketPushEventsConfiguration implements WebSocketMessageBrokerConfigurer {

    public @Override void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/rpc");
        config.setApplicationDestinationPrefixes("/api");
    }

    public @Override void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        IpHandshakeInterceptor ipHeaderInterceptor = new IpHandshakeInterceptor();

        registry.addEndpoint("/ws").setAllowedOrigins("*").addInterceptors(ipHeaderInterceptor);
        registry.addEndpoint("/ws").setAllowedOrigins("*").addInterceptors(ipHeaderInterceptor)
                .withSockJS();
    }

    public class IpHandshakeInterceptor implements HandshakeInterceptor {

        public @Override boolean beforeHandshake(ServerHttpRequest request,
                ServerHttpResponse response, WebSocketHandler wsHandler,
                Map<String, Object> attributes) throws Exception {

            // Set ip attribute to WebSocket session
            if (request instanceof ServletServerHttpRequest) {
                ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                String ipAddress = servletRequest.getServletRequest().getHeader("X-FORWARDED-FOR");
                if (ipAddress == null) {
                    ipAddress = servletRequest.getServletRequest().getRemoteAddr();
                }
                attributes.put("client-ip-address", ipAddress);
            }
            return true;
        }

        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                WebSocketHandler wsHandler, Exception exception) {
        }
    }
}
