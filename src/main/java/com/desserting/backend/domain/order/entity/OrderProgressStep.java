package com.desserting.backend.domain.order.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 고객 화면의 주문 진행 단계 (ORD-03: 주문 접수 → 준비 중 → 준비 완료) */
@Getter
@RequiredArgsConstructor
public enum OrderProgressStep {
    RECEIVED("주문 접수"),
    PREPARING("준비 중"),
    READY("준비 완료");

    private final String label;
}
