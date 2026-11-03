package com.desserting.backend.domain.heart.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 하트 구매 상품 (HRT-05). 가격은 개당 단가 × 수량으로 계산한다 (단가는 desserting.heart.unit-price).
 */
@Getter
@RequiredArgsConstructor
public enum HeartPackage {
    HEARTS_5(5),
    HEARTS_10(10),
    HEARTS_30(30);

    private final int quantity;

    public int priceOf(int unitPrice) {
        return quantity * unitPrice;
    }
}
