package com.desserting.backend.domain.user.entity;

import com.desserting.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(length = 10)
    private String gender; // MALE, FEMALE

    private Integer age;

    @Column(length = 50)
    private String region; // 예: 대구 남구, 대구 중구 등

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(nullable = false)
    @Builder.Default
    private Integer heartCount = 5; // 기본 무료 하트 5개 제공

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPremium = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDummy = false;

    @Column(length = 500)
    private String dessertAvatarUrl;

    @Column(length = 255)
    private String bio; // 한 줄 자기소개

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private UserRole role = UserRole.ROLE_USER;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserDessertTag> dessertTags = new ArrayList<>();

    // Business Methods
    public void updateProfile(String nickname, String bio, String region, String dessertAvatarUrl) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
        if (bio != null) {
            this.bio = bio;
        }
        if (region != null && !region.isBlank()) {
            this.region = region;
        }
        if (dessertAvatarUrl != null && !dessertAvatarUrl.isBlank()) {
            this.dessertAvatarUrl = dessertAvatarUrl;
        }
    }

    public void updateLocation(BigDecimal latitude, BigDecimal longitude, String region) {
        this.latitude = latitude;
        this.longitude = longitude;
        if (region != null) {
            this.region = region;
        }
    }

    public void deductHeart(int count) {
        if (this.isPremium) {
            return; // 프리미엄 회원은 무제한
        }
        if (this.heartCount < count) {
            throw new IllegalStateException("보유 하트가 부족합니다.");
        }
        this.heartCount -= count;
    }

    public void addHeart(int count) {
        this.heartCount += count;
    }

    public void resetDailyHearts(int targetCount) {
        if (this.heartCount < targetCount) {
            this.heartCount = targetCount;
        }
    }
}
