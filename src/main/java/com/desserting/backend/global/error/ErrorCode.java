package com.desserting.backend.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "허용되지 않은 HTTP 메서드입니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "대상을 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C004", "서버 내부 오류가 발생했습니다."),

    // Auth & User
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증에 실패하였습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A002", "접근 권한이 없습니다."),
    EXPIRED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "만료된 JWT 토큰입니다."),
    INVALID_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "유효하지 않은 JWT 토큰입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U002", "이미 존재하는 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "U003", "이미 존재하는 닉네임입니다."),

    // Heart & Matching
    INSUFFICIENT_HEART(HttpStatus.BAD_REQUEST, "H001", "보유 하트가 부족합니다."),
    DUPLICATE_CHAT_REQUEST(HttpStatus.CONFLICT, "H002", "이미 진행 중인 대화 요청이 존재합니다."),
    CHAT_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "H003", "대화 요청을 찾을 수 없습니다."),
    CANNOT_REQUEST_SELF(HttpStatus.BAD_REQUEST, "H004", "본인에게는 대화 요청을 보낼 수 없습니다."),

    // Chat
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CH001", "채팅방을 찾을 수 없습니다."),
    CHAT_MEMBER_NOT_ALLOWED(HttpStatus.FORBIDDEN, "CH002", "해당 채팅방에 참여 권한이 없습니다."),

    // Store & Theme
    THEME_STREET_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "테마거리를 찾을 수 없습니다."),
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "매장을 찾을 수 없습니다."),
    STORE_MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "S002", "메뉴를 찾을 수 없습니다."),
    RESERVATION_SLOT_FULL(HttpStatus.CONFLICT, "R001", "선택하신 시간대의 예약 정원이 마감되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
