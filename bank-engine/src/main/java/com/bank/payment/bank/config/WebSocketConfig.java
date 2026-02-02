package com.bank.payment.bank.config;

import com.bank.payment.bank.websocket.BalanceWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket Configuration for real-time balance updates.
 * Clients can connect to /ws/balance/{accountNumber} to receive real-time
 * balance changes.
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final BalanceWebSocketHandler balanceWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(balanceWebSocketHandler, "/ws/balance/{accountNumber}")
                .setAllowedOrigins("*"); // Allow all origins for development
    }
}
