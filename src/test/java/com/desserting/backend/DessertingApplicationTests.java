package com.desserting.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
class DessertingApplicationTests {

    @Test
    @DisplayName("스프링 부트 애플리케이션 및 멀티DB 컨텍스트 정상 로딩 검증")
    void contextLoads() {
        assertThat(true).isTrue();
    }
}
