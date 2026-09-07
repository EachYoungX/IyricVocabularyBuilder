package com.each17.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.util.Arrays;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer webConfigurer(
            @Value("${app.cors.allowed-origins:}") String configuredOrigins,
            @Value("${app.web.root:}") String configuredWebRoot
    ) {
        String[] allowedOrigins = Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                if (allowedOrigins.length == 0) {
                    return;
                }
                registry.addMapping("/api/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                if (configuredWebRoot.isBlank()) return;
                String resourceLocation = Path.of(configuredWebRoot).toAbsolutePath().normalize().toUri().toString();
                if (!resourceLocation.endsWith("/")) resourceLocation += "/";
                registry.addResourceHandler("/**")
                        .addResourceLocations(resourceLocation)
                        .setCacheControl(CacheControl.noCache());
            }

            @Override
            public void addViewControllers(ViewControllerRegistry registry) {
                if (!configuredWebRoot.isBlank()) {
                    registry.addViewController("/").setViewName("forward:/index.html");
                }
            }
        };
    }
}
