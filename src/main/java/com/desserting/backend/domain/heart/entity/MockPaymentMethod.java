package com.desserting.backend.domain.heart.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 모의 결제 수단 (실제 PG 연동 없음, PAY-03 은 추후 개발) */
@Getter
@RequiredArgsConstructor
public enum MockPaymentMethod {
    CARD("카드 결제(모의)"),
    EASY_PAY("간편 결제(모의)");

    private final String description;
}
