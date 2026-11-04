package com.desserting.backend.domain.order.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 4-7 주문의 결제 상태.
 * 매장 준비 단계(접수 → 준비 중 → 방문 완료)는 예약 상태(ReservationStatus)를 그대로 따른다.
 */
@Getter
@RequiredArgsConstructor
public enum OrderStatus {
    PAID("결제 완료(모의)"),
    REFUNDED("환불 완료(모의)");

    private final String description;
}
