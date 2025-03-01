package com.example.demo.entity;

import jakarta.persistence.PrePersist;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Setter
@MappedSuperclass
@NoArgsConstructor
public class BaseEntity {

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private static final String DEFAULT_TIMEZONE = "Asia/Seoul"; // 기본 시간대

    @PrePersist
    public void onCreate() {
        ZoneId zoneId = ZoneId.of(DEFAULT_TIMEZONE); // 고정된 시간대 사용
        this.createdAt = LocalDateTime.now(zoneId);
        this.updatedAt = LocalDateTime.now(zoneId);
    }

    @PreUpdate
    public void onUpdate() {
        ZoneId zoneId = ZoneId.of(DEFAULT_TIMEZONE); // 고정된 시간대 사용
        this.updatedAt = LocalDateTime.now(zoneId);
    }
}