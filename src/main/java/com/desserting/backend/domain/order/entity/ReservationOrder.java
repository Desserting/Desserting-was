package com.desserting.backend.domain.order.entity;

import com.desserting.backend.domain.heart.entity.MockPaymentMethod;
import com.desserting.backend.domain.reservation.entity.Reservation;
import com.desserting.backend.domain.store.entity.Store;
import com.desserting.backend.domain.store.entity.StoreCoupon;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 4-7 예약 메뉴 선주문 + 모의 결제 (ORD-01).
 * 예약 1건당 주문 1건이며, 결제 완료 시 예약의 totalAmount/discountAmount/finalAmount 에도 금액이 반영된다.
 */
@Entity
@Table(name = "reservation_orders",
        uniqueConstraints = @UniqueConstraint(name = "uk_reservation_order_reservation", columnNames = "reservation_id"),
        indexes = {
                @Index(name = "idx_reservation_order_user", columnList = "user_id"),
                @Index(name = "idx_reservation_order_store", columnList = "store_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ReservationOrder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private StoreCoupon coupon;

    /** 쿠폰이 나중에 수정·삭제돼도 영수증에 남도록 이름을 따로 보관 */
    @Column(length = 100)
    private String couponName;

    @Column(nullable = false)
    private Integer totalAmount;

    @Column(nullable = false)
    private Integer discountAmount;

    @Column(nullable = false)
    private Integer finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentType paymentType;

    /** 개별 결제 시 나눠 내는 인원 (합산 결제는 1) */
    @Column(nullable = false)
    private Integer splitCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MockPaymentMethod paymentMethod;

    /** 모의 결제 승인 번호 */
    @Column(nullable = false, unique = true, length = 50)
    private String paymentKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false)
    private LocalDateTime paidAt;

    private LocalDateTime refundedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<ReservationOrderItem> items = new ArrayList<>();

    public void addItem(ReservationOrderItem item) {
        items.add(item);
        item.assignOrder(this);
    }

    public boolean refund(LocalDateTime now) {
        if (status != OrderStatus.PAID) {
            return false;
        }
        this.status = OrderStatus.REFUNDED;
        this.refundedAt = now;
        return true;
    }

    /** 1인당 결제 금액 (합산 결제면 1개) */
    public List<Integer> shareAmounts() {
        return splitAmounts(finalAmount, splitCount);
    }

    /** 금액을 count명으로 나눈다. 나머지 원 단위는 앞사람부터 1원씩 더 낸다. */
    public static List<Integer> splitAmounts(int amount, int count) {
        int n = Math.max(1, count);
        List<Integer> shares = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            shares.add(amount / n + (i < amount % n ? 1 : 0));
        }
        return shares;
    }
}
