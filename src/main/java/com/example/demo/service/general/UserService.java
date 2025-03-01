package com.example.demo.service.general;

import com.example.demo.base.ApiResponse;

public interface UserService {

    // 1. 로그아웃 : 유저 아이디로 로그아웃 처리
    ApiResponse<?> logout(String userId);

    // 2. 회원 탈퇴 : 유저 아이디로 회원 탈퇴 처리
    ApiResponse<?> deleteUser(String userId);

    // 3. 사용자 -> 관리자 전환
    ApiResponse<?> turnAdmin(String userId);

}