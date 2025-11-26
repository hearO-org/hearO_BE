package com.hearo.alert.domain;

import com.hearo.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "device_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class DeviceToken extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;        // User 엔티티와 FK 맺어도 되고, ID만 저장도 가능

    @Column(nullable = false, length = 512, unique = true)
    private String token;       // FCM device token

    @Column(nullable = false, length = 20)
    private String platform;    // "ANDROID", "IOS", "WEB" 등

    @Column(nullable = false)
    private boolean active;     // 앱 삭제/로그아웃 시 false 처리
}
