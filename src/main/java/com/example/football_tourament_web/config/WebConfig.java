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
        Path persistentUploadDir = Paths.get(System.getProperty("user.home"), ".football_tournament_web", "uploads").toAbsolutePath().normalize();
        Path externalUploadDir = Paths.get("uploads").toAbsolutePath().normalize();
        Path legacyUploadDir = Paths.get("src", "main", "resources", "static", "uploads").toAbsolutePath().normalize();
        String persistentLocation = ensureTrailingSlash(persistentUploadDir.toUri().toString());
        String externalLocation = ensureTrailingSlash(externalUploadDir.toUri().toString());
        String legacyLocation = ensureTrailingSlash(legacyUploadDir.toUri().toString());

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(persistentLocation, externalLocation, legacyLocation);
    }

    private String ensureTrailingSlash(String value) {
        return value.endsWith("/") ? value : value + "/";
    }
}
