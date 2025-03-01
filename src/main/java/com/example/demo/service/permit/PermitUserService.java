package com.example.demo.service.permit;

import com.example.demo.base.ApiResponse;
import com.example.demo.domain.dto.TokenResponse;
import com.example.demo.domain.dto.UserRequest;
import org.springframework.http.ResponseEntity;

public interface PermitUserService {

    // 1. 회원가입 : UserRequest를 받아 회원가입 후 토큰 반환
    ResponseEntity<ApiResponse<TokenResponse>> signup(UserRequest userRequest);

    // 2. 로그인 : 유저 아이디와 비밀번호로 로그인 후 토큰 반환
    ResponseEntity<ApiResponse<TokenResponse>> login(String userId, String password);

    // 3. 리프레시 토큰 갱신 : 유효한 리프레시 토큰을 받아 새 토큰 반환
    ResponseEntity<ApiResponse<TokenResponse>> refreshToken(String refreshToken);

    // 4. 소셜 로그인 : 이름으로 토큰 조회
    ResponseEntity<ApiResponse<TokenResponse>> socialLogin(String userName);

}