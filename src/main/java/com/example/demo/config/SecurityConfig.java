package com.example.demo.config;

import com.example.demo.repository.TokenRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.jwt.JwtAuthenticationFilter;
import com.example.demo.security.jwt.JwtUtil;
import com.example.demo.security.oauth.access.CommonOAuthHandler;
import com.example.demo.security.oauth.authentication.OAuthLoginFailureHandler;
import com.example.demo.security.oauth.authentication.OAuthLoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Collections;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;

    // 소셜 로그인
    private final OAuthLoginSuccessHandler oAuthLoginSuccessHandler;
    private final OAuthLoginFailureHandler oAuthLoginFailureHandler;
    private final CommonOAuthHandler customOAuthHandlerFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .httpBasic(HttpBasicConfigurer::disable)
                .cors(corsConfigurer -> corsConfigurer.configurationSource(corsConfigurationSource()))  // CORS 설정
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(customOAuthHandlerFilter, UsernamePasswordAuthenticationFilter.class) // OAuth 핸들러 ( 소셜 로그인 )
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, userRepository, tokenRepository), UsernamePasswordAuthenticationFilter.class)  // JwtAuthenticationFilter
                .authorizeHttpRequests(authorize -> {
                    authorize
                            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()  // Swagger 관련 경로 허용
                            .requestMatchers("/api/permit/**").permitAll()  // 누구나 접근할 수 있는 엔드포인트
                            .anyRequest().authenticated();  // 나머지 요청은 인증을 요구
                })
                .oauth2Login(oauth -> {
                    oauth
                            .successHandler(oAuthLoginSuccessHandler)
                            .failureHandler(oAuthLoginFailureHandler);
                });
        return httpSecurity.build();
    }

    CorsConfigurationSource corsConfigurationSource() {
        return request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedHeaders(Collections.singletonList("*"));
            config.setAllowedMethods(Collections.singletonList("*"));
            config.setAllowedOriginPatterns(Collections.singletonList("*"));
            config.setAllowCredentials(true);
            return config;
        };
    }
}