
package com.bank.payment.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class UserEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserEngineApplication.class, args);
    }
}
