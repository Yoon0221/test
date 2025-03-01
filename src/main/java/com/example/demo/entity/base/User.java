package com.example.demo.entity.base;

import com.example.demo.entity.enums.Provider;
import com.example.demo.entity.enums.Status;
import com.example.demo.entity.enums.Role;
import com.example.demo.entity.relations.UserRelations;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends UserRelations {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // name 은 20자 이내, 고유하게 설정
    @Column(nullable = false, length = 20, unique = true)
    private String userName;

    // userId 는 20자 이내, 고유하게 설정
    @Column(nullable = false, length = 20, unique = true)
    private String userId;

    // password 는 20자 이내로 설정
    @Column(nullable = false, length = 20)
    private String password;

    // provider 는 4개 ( SELF 혹은 소셜로그인 타입 ) 중 하나
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    // role 은 2개 ( 관리자 혹은 유저. 유저는 관리자 api 에 접근 불가. 관리자는 유저 api 에 접근 가능 ) 중 하나
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // active 는 3개 ( 활성 = 로그인, 비활성 = 로그아웃, 탈퇴 = 일정시간 이상 지속시 자동으로 회원 정보 삭제 ) 중 하나
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status active;

    // 탈퇴 시간 기록 ( 이 시간으로부터 일정 시간 이상 지나면 자동으로 사용자 정보가 삭제됨. 재로그인시 탈퇴 시간이 삭제되고 활성화 상태로 설정됨. )
    @Column
    private LocalDateTime withdrawTime;

    // 메서드

    // 1. 사용자 상태를 탈퇴로 설정 & 탈퇴 시간 기록 로직
    public void withdraw() {
        this.active = Status.WITHDRAWN;
        this.withdrawTime = LocalDateTime.now();
    }

    // 2. 사용자 상태를 활성화로 설정 & 탈퇴 시간을 삭제하는 로직
    public void activate() {
        this.active = Status.ACTIVE;
        this.withdrawTime = null;
    }

    // 3. 사용자 상태를 비활성화로 설정하는 로직
    public void deactivate() {
        this.active = Status.INACTIVE;
    }

}