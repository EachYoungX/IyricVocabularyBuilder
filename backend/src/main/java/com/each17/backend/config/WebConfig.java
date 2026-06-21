package com.each17.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**") // 允许 /api/ 下的所有路径
                        .allowedOrigins("http://localhost:9000") // 允许来自前端开发服务器的请求
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许所有常用方法
                        .allowedHeaders("*") // 允许所有请求头
                        .allowCredentials(true); // 是否允许发送 Cookie
            }
        };
    }
}