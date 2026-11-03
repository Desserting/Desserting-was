package com.desserting.backend.domain.health.controller;

import com.desserting.backend.global.common.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HealthControllerTest {

    @Test
    @DisplayName("헬스 체크 API 정상 응답 검증")
    void checkHealth() {
        // given
        HealthController controller = new HealthController();
        ReflectionTestUtils.setField(controller, "activeProfile", "local");

        // when
        ApiResponse<Map<String, Object>> response = controller.checkHealth();

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("서버가 정상 동작 중입니다.");
        assertThat(response.getData().get("status")).isEqualTo("UP");
        assertThat(response.getData().get("profile")).isEqualTo("local");
    }
}
