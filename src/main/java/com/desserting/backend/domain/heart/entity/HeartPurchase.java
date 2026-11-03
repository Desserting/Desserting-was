package com.desserting.backend.domain.heart.entity;

import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 하트 구매(모의 결제) 내역. 하트 잔액 변동 자체는 HeartHistory(PURCHASE)에도 함께 기록된다.
 */
@Entity
@Table(name = "heart_purchases", indexes = @Index(name = "idx_heart_purchase_user", columnList = "user_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class HeartPurchase extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HeartPackage heartPackage;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer unitPrice;

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MockPaymentMethod paymentMethod;

    /** 모의 결제 승인 번호 */
    @Column(nullable = false, unique = true, length = 50)
    private String paymentKey;

    @Column(nullable = false)
    private Integer heartCountAfter;
}
