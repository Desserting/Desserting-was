package com.desserting.backend.domain.heart.entity;

import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "heart_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class HeartHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer amount; // 차감 시 -1, 충전 시 +5 등

    @Column(nullable = false)
    private Integer balanceAfter; // 변동 후 잔여 하트 수

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HeartHistoryType type;

    @Column(length = 255)
    private String description;

    public static HeartHistory of(User user, int amount, int balanceAfter, HeartHistoryType type, String description) {
        return HeartHistory.builder()
                .user(user)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .type(type)
                .description(description)
                .build();
    }
}
