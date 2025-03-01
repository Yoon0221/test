package com.example.demo.controller.general;

import com.example.demo.base.ApiResponse;
import com.example.demo.controller.BaseController;
import com.example.demo.service.general.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController extends BaseController {

    @Autowired
    private UserService userService;

    // 1. 로그아웃 API
    @PatchMapping("/logout")
    public ApiResponse<?> logout() {
        String userId = getCurrentUserId();  // 현재 로그인 된 사용자 ID
        return userService.logout(userId);
    }

    // 2. 회원탈퇴 API
    @DeleteMapping("/delete")
    public ApiResponse<?> deleteUser() {
        String userId = getCurrentUserId();  // 현재 로그인 된 사용자 ID
        return userService.deleteUser(userId);
    }

    // 3. 사용자 -> 관리자 전환 API
    @PatchMapping("/turn-admin")
    public ApiResponse<?> turnAdmin() {
        String userId = getCurrentUserId(); // 현재 로그인 된 사용자 ID
        return userService.turnAdmin(userId);
    }

}