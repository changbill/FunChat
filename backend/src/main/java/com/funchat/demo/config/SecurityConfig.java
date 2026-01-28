package com.funchat.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 보호 비활성화 (개발 초기 API 테스트를 편하게 하기 위함)
                .csrf(AbstractHttpConfigurer::disable)
                // 2. 모든 요청에 대해 인증 없이 접근 허용
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                // 3. 기본 로그인 폼 비활성화
                .formLogin(AbstractHttpConfigurer::disable)
                // 4. HTTP 기본 인증 비활성화
                .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
