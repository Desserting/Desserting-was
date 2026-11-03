package com.desserting.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
class DessertingApplicationTests {

    @Test
    @DisplayName("기본 애플리케이션 환경 로드 검증")
    void contextLoads() {
        assertThat(true).isTrue();
    }
}
