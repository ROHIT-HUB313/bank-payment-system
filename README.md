# Bank Payment Microservices System

A distributed banking system built with Spring Boot, Spring Cloud, and Gradle.

## Architecture
-   **Discovery Engine**: Eureka Server
-   **API Gateway Engine**: Entry point, JWT Auth, Routing
-   **User Engine**: Authentication & User Management
-   **Bank Engine**: Account Management
-   **Transaction Engine**: Money Transfers
-   **Admin Engine**: Aggregator for Management

## Setup
1.  Ensure MySQL is running.
2.  Import project into IntelliJ IDEA or Eclipse as a Gradle project.
3.  Run the modules in order: Discovery -> Services -> Gateway.

See [walkthrough.md](../brain/cfe180be-1a68-49b6-a329-bc019d3646a4/walkthrough.md) for detailed running instructions.
