package com.amartya.seatlock.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${seatlock.allowed-origin:http://localhost:5173}") private String allowedOrigin;
    @Value("${FRONTEND_HOST:}") private String frontendHost;
    @Override public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = new ArrayList<>(List.of(allowedOrigin));
        // Render's `host` property is a bare service name, so add the public domain when missing.
        if (!frontendHost.isBlank()) origins.add("https://" + (frontendHost.contains(".") ? frontendHost : frontendHost + ".onrender.com"));
        registry.addMapping("/api/**").allowedOrigins(origins.toArray(String[]::new)).allowedMethods("GET", "POST", "OPTIONS").allowedHeaders("*");
    }
}
