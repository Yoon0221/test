package com.example.demo.domain.converter;

import com.example.demo.domain.dto.TokenResponse;
import com.example.demo.entity.base.Token;
import com.example.demo.entity.base.User;
import org.springframework.stereotype.Component;

@Component
public class TokenConverter {

    // TokenResponse를 Token 엔티티로 변환
    public Token toEntity(String token, String refreshToken, User user) {
        return Token.builder()
                .accessToken(token)
                .refreshToken(refreshToken)
                .user(user)  // 사용자와의 1대 1관계 설정
                .build();
    }

    // Token 엔티티를 TokenResponse DTO로 변환
    public TokenResponse toResponse(Token token) {
        return new TokenResponse(token.getAccessToken(), token.getRefreshToken(), token.getCreatedAt(), token.getUpdatedAt());
    }

}