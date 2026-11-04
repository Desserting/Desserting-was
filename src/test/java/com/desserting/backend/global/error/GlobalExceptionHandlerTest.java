package com.desserting.backend.global.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    @Test
    @DisplayName("NoResourceFoundException 발생 시 404 NOT_FOUND 응답 반환 검증")
    void handleNoResourceFoundException() {
        // given
        NoResourceFoundException exception = new NoResourceFoundException(HttpMethod.GET, "/non-existent-api");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleNoResourceFoundException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.ENTITY_NOT_FOUND.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo("요청한 리소스를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("MissingServletRequestParameterException 발생 시 400 BAD_REQUEST 응답 반환 검증")
    void handleMissingServletRequestParameterException() {
        // given
        MissingServletRequestParameterException exception =
                new MissingServletRequestParameterException("targetUserId", "Long");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleMissingServletRequestParameterException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
        assertThat(response.getBody().getMessage()).contains("targetUserId 파라미터가 누락되었습니다.");
    }
}
