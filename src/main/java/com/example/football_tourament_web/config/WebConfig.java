package com.example.football_tourament_web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get("src", "main", "resources", "static", "uploads").toAbsolutePath().normalize();
        String uploadLocation = ensureTrailingSlash(uploadDir.toUri().toString());

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation);
    }

    private String ensureTrailingSlash(String value) {
        return value.endsWith("/") ? value : value + "/";
    }
}
