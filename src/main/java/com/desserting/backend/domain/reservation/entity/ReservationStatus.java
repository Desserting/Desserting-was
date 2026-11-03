package com.desserting.backend.domain.reservation.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReservationStatus {
    PENDING("예약 대기"),
    CONFIRMED("예약 확정"),
    PREPARING("메뉴 준비 중"),
    COMPLETED("방문 완료"),
    CANCELLED("예약 취소");

    private final String description;
}
