package com.desserting.backend.domain.reservation.entity;

import com.desserting.backend.domain.store.entity.Store;
import com.desserting.backend.domain.store.entity.StoreCoupon;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Reservation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false)
    private LocalDate reservationDate;

    @Column(nullable = false)
    private LocalTime reservationTime;

    @Column(nullable = false)
    @Builder.Default
    private Integer guestCount = 2; // 기본 2인 데이트 예약

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalAmount = 0; // 메뉴 선주문 합산 금액

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private StoreCoupon coupon;

    @Column(nullable = false)
    @Builder.Default
    private Integer discountAmount = 0; // 쿠폰 할인 금액

    @Column(nullable = false)
    @Builder.Default
    private Integer finalAmount = 0; // 할인 적용 후 실결제 금액

    @Column(length = 255)
    private String specialRequest;

    public void updateStatus(ReservationStatus status) {
        if (status != null) {
            this.status = status;
        }
    }

    public void applyCoupon(StoreCoupon coupon, int discount) {
        this.coupon = coupon;
        this.discountAmount = discount;
        this.finalAmount = Math.max(0, this.totalAmount - discount);
    }
}
