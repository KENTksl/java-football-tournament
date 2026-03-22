package com.example.football_tourament_web.config;

//import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean; // <--- Dòng này để nhận diện @Bean
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.MultipartConfigElement; // <--- Dòng này để nhận diện MultipartConfigElement
import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Lấy đường dẫn tuyệt đối đến thư mục uploads
        String uploadPath = new File("src/main/resources/static/uploads").getAbsolutePath();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:///" + uploadPath + File.separator);
    }
}