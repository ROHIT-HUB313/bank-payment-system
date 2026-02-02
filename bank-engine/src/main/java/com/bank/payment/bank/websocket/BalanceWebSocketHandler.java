package com.bank.payment.bank.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket Handler for real-time balance updates.
 * 
 * Clients connect to: ws://host:port/ws/balance/{accountNumber}
 * When balance changes, all connected clients for that account receive an
 * update.
 */
@Component
@Slf4j
public class BalanceWebSocketHandler extends TextWebSocketHandler {

    // Map: accountNumber -> Set of WebSocket sessions
    private final Map<String, Set<WebSocketSession>> accountSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String accountNumber = extractAccountNumber(session);
        if (accountNumber != null) {
            accountSessions.computeIfAbsent(accountNumber, k -> new CopyOnWriteArraySet<>()).add(session);
            log.info("WebSocket connected for account: {}, sessionId: {}", accountNumber, session.getId());

            // Send welcome message
            session.sendMessage(
                    new TextMessage("{\"type\":\"CONNECTED\",\"message\":\"Listening for balance updates\"}"));
        } else {
            log.warn("WebSocket connection without account number, closing");
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String accountNumber = extractAccountNumber(session);
        if (accountNumber != null) {
            Set<WebSocketSession> sessions = accountSessions.get(accountNumber);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    accountSessions.remove(accountNumber);
                }
            }
            log.info("WebSocket disconnected for account: {}, sessionId: {}", accountNumber, session.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Clients can send a PING and receive a PONG
        String payload = message.getPayload();
        if ("PING".equalsIgnoreCase(payload)) {
            session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
        }
    }

    /* Broadcast balance update to all connected clients for an account. Call this method from AccountService after any balance modification.
     * 
     * @param accountNumber   The account number
     * @param newBalance      The updated balance
     * @param transactionType Type of transaction (CREDIT/DEBIT/DEPOSIT/WITHDRAWAL)
     */
    public void broadcastBalanceUpdate(String accountNumber, BigDecimal newBalance, String transactionType) {
        Set<WebSocketSession> sessions = accountSessions.get(accountNumber);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("No WebSocket clients connected for account: {}", accountNumber);
            return;
        }

        String jsonMessage = String.format(
                "{\"type\":\"BALANCE_UPDATE\",\"accountNumber\":\"%s\",\"balance\":%.2f,\"transactionType\":\"%s\",\"timestamp\":\"%s\"}",
                accountNumber, newBalance, transactionType, Instant.now());

        TextMessage message = new TextMessage(jsonMessage);

        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(message);
                    log.info("Balance update sent to session: {} for account: {}", session.getId(), accountNumber);
                }
            } catch (IOException e) {
                log.error("Failed to send balance update to session: {}", session.getId(), e);
            }
        }
    }

    /* Extract account number from the WebSocket URI path. Expected path: /ws/balance/{accountNumber} */
    private String extractAccountNumber(WebSocketSession session) {
        String path = session.getUri().getPath();
        if (path != null && path.startsWith("/ws/balance/")) {
            return path.substring("/ws/balance/".length());
        }
        return null;
    }

    /* Get count of connected sessions for an account (for monitoring). */
    public int getConnectedSessionCount(String accountNumber) {
        Set<WebSocketSession> sessions = accountSessions.get(accountNumber);
        return sessions != null ? sessions.size() : 0;
    }
}
