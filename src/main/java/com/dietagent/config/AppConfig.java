package com.dietagent.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // 调第三方图片 API 必须有超时兜底，避免 Pexels 卡住时拖死流式收尾回调的线程
        return builder
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(8))
                .build();
    }
}
