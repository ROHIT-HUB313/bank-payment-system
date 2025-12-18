
package com.bank.payment.gateway.filter;

import com.bank.payment.gateway.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private RouteValidator validator;

    @Autowired
    private JwtUtil jwtUtil;

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            String path = exchange.getRequest().getPath().toString();

            if (validator.isSecured.test(exchange.getRequest())) {
                log.debug("Secured endpoint accessed: {}", path);

                if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    log.error("Missing authorization header for path: {}", path);
                    throw new RuntimeException("missing authorization header");
                }

                String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    authHeader = authHeader.substring(7);
                }

                try {
                    jwtUtil.validateToken(authHeader);

                    String userId = String.valueOf(jwtUtil.extractUserId(authHeader));
                    String username = jwtUtil.extractUsername(authHeader);
                    String role = jwtUtil.extractRole(authHeader);

                    log.info("Request authenticated: path={}, userId={}, username={}, role={}",
                            path, userId, username, role);

                    exchange = exchange.mutate()
                            .request(exchange.getRequest().mutate()
                                    .header("X-User-Id", userId)
                                    .header("X-User-Name", username)
                                    .header("X-User-Role", role)
                                    .build())
                            .build();

                } catch (Exception e) {
                    log.error("Authentication failed for path {}: {}", path, e.getMessage());
                    throw new RuntimeException("unauthorized access to application");
                }
            } else {
                log.debug("Public endpoint accessed: {}", path);
            }
            return chain.filter(exchange);
        });
    }

    public static class Config {

    }
}
