package com.desserting.backend.domain.order.entity;

import com.desserting.backend.domain.store.entity.StoreMenu;
import com.desserting.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/** 주문 메뉴 1줄. 메뉴명·단가는 주문 시점 값으로 보관한다 (이후 메뉴 수정·삭제 영향 없음). */
@Entity
@Table(name = "reservation_order_items", indexes = @Index(name = "idx_order_item_order", columnList = "order_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ReservationOrderItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private ReservationOrder order;

    /** 메뉴가 삭제돼도 주문 내역은 남도록 FK 없이 ID만 보관 */
    @Column(nullable = false)
    private Long menuId;

    @Column(nullable = false, length = 100)
    private String menuName;

    @Column(nullable = false)
    private Integer unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer lineAmount;

    public static ReservationOrderItem of(StoreMenu menu, int quantity) {
        return ReservationOrderItem.builder()
                .menuId(menu.getId())
                .menuName(menu.getName())
                .unitPrice(menu.getPrice())
                .quantity(quantity)
                .lineAmount(menu.getPrice() * quantity)
                .build();
    }

    void assignOrder(ReservationOrder order) {
        this.order = order;
    }
}
