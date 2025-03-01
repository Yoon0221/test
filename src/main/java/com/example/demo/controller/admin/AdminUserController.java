package com.example.demo.controller.admin;

import com.example.demo.controller.BaseController;
import com.example.demo.base.ApiResponse;
import com.example.demo.service.admin.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController extends BaseController {

    @Autowired
    private AdminUserService adminUserService;

    // 1. 관리자 -> 사용자 전환 API
    @PatchMapping("/turn-user")
    public ApiResponse<?> turnUser() {
        String userId = getCurrentUserId(); // 현재 로그인 된 사용자 ID
        return adminUserService.turnUser(userId);
    }

    // 2. 전체 사용자 조회 API
    @GetMapping("/get/all-user")
    public ApiResponse<?> getAllUsers() {
        return adminUserService.getAllUsers();
    }


}