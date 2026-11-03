package com.desserting.backend.global.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("AccessDeniedException 발생 시 403 FORBIDDEN 응답 반환 검증")
    void handleAccessDeniedException() {
        // given
        AccessDeniedException exception = new AccessDeniedException("접근 권한이 없습니다.");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleAccessDeniedException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.ACCESS_DENIED.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo("접근 권한이 없습니다.");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException 발생 시 400 BAD_REQUEST 응답 반환 검증")
    void handleHttpMessageNotReadableException() {
        // given
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException("JSON parse error");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadableException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo("요청 본문(JSON) 형식이 올바르지 않습니다.");
    }
}
