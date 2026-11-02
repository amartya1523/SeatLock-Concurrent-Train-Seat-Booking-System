package com.amartya.seatlock.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {
    @Bean OpenAPI seatLockApi() {
        return new OpenAPI().info(new Info().title("SeatLock API").version("1.0.0").description("Race-safe temporary seat locking and booking API").contact(new Contact().name("Amartya Singh")));
    }
}
