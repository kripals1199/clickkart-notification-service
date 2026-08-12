// src/main/java/com/clickkart/notification/config/OpenApiConfig.java
package com.clickkart.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Backs the Swagger UI at {@code /swagger-ui.html}. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ClickKart Notification Service")
                        .version("1.0.0")
                        .description("Dispatches password-reset/OTP/verification-code notifications on behalf of other services (simulated dispatch)."));
    }
}
