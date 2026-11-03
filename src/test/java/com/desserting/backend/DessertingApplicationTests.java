package com.desserting.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class DessertingApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("스프링 부트 애플리케이션 및 멀티DB 컨텍스트 정상 로딩 검증")
    void contextLoads() {
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("Swagger OpenAPI 3.0 명세서 (/v3/api-docs) 정상 생성 및 최신 엔드포인트 포함 검증")
    void openApiDocs_GenerateSuccessfully() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value("3.0.1"))
                .andExpect(jsonPath("$.info.title").isNotEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/matches/random']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/themes']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/stores']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/reservations']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/signup']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hearts/history']").exists());
    }
}
