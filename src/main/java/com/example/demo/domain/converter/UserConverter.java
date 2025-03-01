package com.example.demo.domain.converter;

import com.example.demo.domain.dto.UserRequest;
import com.example.demo.entity.base.User;
import com.example.demo.entity.enums.Role;
import com.example.demo.entity.enums.Status;
import org.springframework.stereotype.Component;

@Component
public class UserConverter {

    // UserRequest를 User 엔티티로 변환
    public User toEntity(UserRequest userRequest) {
        return User.builder()
                .userId(userRequest.getUserId())
                .password(userRequest.getPassword())
                .userName(userRequest.getName())
                .provider(userRequest.getProvider())
                .role(Role.USER)  // 기본적으로 USER로 설정
                .active(Status.ACTIVE)  // 기본적으로 ACTIVE로 설정
                .build();
    }

}