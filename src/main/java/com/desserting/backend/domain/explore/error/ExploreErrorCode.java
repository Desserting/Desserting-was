package com.desserting.backend.domain.explore.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 탐색 도메인(4-2 매칭 · 4-5 지도 · 4-6 매장/예약) 전용 에러 코드.
 * 공통 ErrorCode 와 코드값이 겹치지 않도록 EX 접두어를 사용한다.
 */
@Getter
@RequiredArgsConstructor
public enum ExploreErrorCode {

    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "EX001", "요청 파라미터가 올바르지 않습니다."),
    INVALID_MATCH_FILTER(HttpStatus.BAD_REQUEST, "EX101", "매칭 필터 조건이 올바르지 않습니다."),
    INVALID_MAP_BOUNDS(HttpStatus.BAD_REQUEST, "EX201", "지도 영역은 swLat ≤ neLat, swLng ≤ neLng 인 네 값을 모두 보내야 합니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "EX301", "조회 기간이 올바르지 않습니다."),
    RESERVATION_SLOT_PASSED(HttpStatus.BAD_REQUEST, "EX302", "이미 지난 시간대는 예약할 수 없습니다."),
    DUPLICATE_RESERVATION(HttpStatus.CONFLICT, "EX303", "이미 같은 시간대에 예약이 존재합니다."),
    RESERVATION_NOT_CANCELLABLE(HttpStatus.BAD_REQUEST, "EX304", "취소할 수 없는 예약 상태입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
