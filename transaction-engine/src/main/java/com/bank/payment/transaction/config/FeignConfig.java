
package com.bank.payment.transaction.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null) {
                    // needed if spring security is added to bank-engine
                    template.header("Authorization", authHeader);
                }
                String userId = request.getHeader("X-User-Id");
                if (userId != null) {
                    template.header("X-User-Id", userId);
                }
            }
            // Inject Internal Secret for Inter-Service Communication
            template.header("X-Internal-Secret", "bank-payment-system-internal-secret");
        };
    }
}
