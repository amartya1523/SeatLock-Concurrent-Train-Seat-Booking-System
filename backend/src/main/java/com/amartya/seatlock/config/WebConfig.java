package com.amartya.seatlock.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${seatlock.allowed-origin:http://localhost:5173}") private String allowedOrigin;
    @Override public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins(allowedOrigin).allowedMethods("GET", "POST", "OPTIONS").allowedHeaders("*");
    }
}
