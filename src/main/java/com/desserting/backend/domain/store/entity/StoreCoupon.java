package com.desserting.backend.domain.store.entity;

import com.desserting.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * 4-6. 제휴매장 자율 할인쿠폰 엔티티 (SHOP-03, RSV-04)
 * 점주가 자율적으로 등록하여 예약/주문 시 할인을 제공하며, 7% 플랫폼 수수료는 할인 후 실결제액 기준으로 산정된다.
 */
@Entity
@Table(name = "store_coupons", indexes = @Index(name = "idx_store_coupon_store", columnList = "store_id, is_active"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class StoreCoupon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 100)
    private String couponName;

    @Column(nullable = false)
    private Integer discountAmount; // 할인 금액 (원)

    @Column(nullable = false)
    @Builder.Default
    private Integer minOrderAmount = 0; // 최소 주문 금액 (원)

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    private LocalDate expiryDate; // 만료일자 (null이면 무기한)

    public void toggleActive() {
        this.isActive = !Boolean.TRUE.equals(this.isActive);
    }

    public boolean isUsable(int orderAmount) {
        if (!Boolean.TRUE.equals(this.isActive)) {
            return false;
        }
        if (expiryDate != null && expiryDate.isBefore(LocalDate.now())) {
            return false;
        }
        return orderAmount >= (minOrderAmount != null ? minOrderAmount : 0);
    }
}
