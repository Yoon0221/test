package com.example.demo.service.admin;

import com.example.demo.base.ApiResponse;

public interface AdminUserService {

    // 1. 관리자 -> 유저 전환
    ApiResponse<?> turnUser(String userId);

    // 2. 전체 사용자 조회
    ApiResponse<?> getAllUsers();

}