package com.desserting.backend.domain.matching.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RequestStatus {
    PENDING("대화 요청 대기 중"),
    ACCEPTED("대화 요청 수락됨"),
    REJECTED("대화 요청 거절됨"),
    EXPIRED("대화 요청 기한 만료");

    private final String description;
}
