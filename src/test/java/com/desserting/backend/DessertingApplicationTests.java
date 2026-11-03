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
    @DisplayName("Swagger OpenAPI 3.0 명세서 (/v3/api-docs) 정상 생성 및 표준화된 태그 포함 검증")
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
                .andExpect(jsonPath("$.paths['/api/v1/hearts/history']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hearts/packages']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/hearts/purchase']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me/visitors']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/matches/history']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/stores/{storeId}/coupons']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/stores/{storeId}/coupons']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/chats/rooms/{roomId}/ai-recommendation']").exists())
                .andExpect(jsonPath("$.tags[?(@.name == '00. 헬스 체크 API')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == '4-1. 계정 인증 및 토큰 API')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == '4-1. 사용자 및 디저트 프로필 API')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == '4-2. 랜덤 매칭 및 주변 탐색 API')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == '4-3. 대화 요청 관리 API')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == '4-3. 하트 경제 및 충전 API')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == '4-4. 실시간 대화 및 채팅방 API')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == '4-4. 소셜 인터랙션 및 안전 API')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == '4-5. 테마거리 지도 및 구역 API')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == '4-6. 제휴매장 검색 및 상세 API')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == '4-6. 매장 테이블 예약 API')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == '4-8. 매장 관리자 및 7%% 정산 API')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == '4-8. 점주 실시간 예약 주문 관리 API')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == '4-8. 테마거리 운영 및 구역 관리 API')]").exists());
    }
}
