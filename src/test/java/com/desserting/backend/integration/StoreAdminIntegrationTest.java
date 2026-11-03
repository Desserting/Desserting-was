package com.desserting.backend.integration;

import com.desserting.backend.domain.admin.dto.ReservationSlotCreateRequest;
import com.desserting.backend.domain.admin.dto.StoreCreateRequest;
import com.desserting.backend.domain.admin.dto.StoreMenuCreateRequest;
import com.desserting.backend.domain.auth.dto.LoginRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class StoreAdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        // 관리자 계정 로그인하여 AccessToken 획득
        LoginRequest loginRequest = LoginRequest.builder()
                .email("admin@desserting.com")
                .password("admin1234!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        adminToken = jsonNode.get("data").get("accessToken").asText();
    }

    @Test
    @DisplayName("[통합 E2E] 관리자 매장 등록 -> 메뉴 추가 -> 품절 토글 -> 슬롯 개설 전체 흐름 검증")
    void storeAdminFullFlow_Success() throws Exception {
        String randomSuffix = UUID.randomUUID().toString().substring(0, 5);
        String storeName = "통합테스트카페_" + randomSuffix;

        // 1. 매장 등록 (POST /api/v1/admin/stores)
        StoreCreateRequest storeCreateRequest = StoreCreateRequest.builder()
                .name(storeName)
                .address("대구 중구 중앙대로 100")
                .latitude(new BigDecimal("35.8690000"))
                .longitude(new BigDecimal("128.5990000"))
                .businessHours("12:00 - 21:00")
                .contact("053-111-2222")
                .partnerDiscountNotice("디저팅 회원 10% 할인")
                .isPartner(true)
                .build();

        MvcResult storeResult = mockMvc.perform(post("/api/v1/admin/stores")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storeCreateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value(storeName))
                .andExpect(jsonPath("$.data.isPartner").value(true))
                .andReturn();

        Long storeId = objectMapper.readTree(storeResult.getResponse().getContentAsString())
                .get("data").get("storeId").asLong();

        // 2. 메뉴 추가 (POST /api/v1/admin/stores/{storeId}/menus)
        StoreMenuCreateRequest menuCreateRequest = StoreMenuCreateRequest.builder()
                .name("수제 크루키")
                .price(6500)
                .description("바삭한 크루아상 쿠키")
                .isRepresentative(true)
                .build();

        MvcResult menuResult = mockMvc.perform(post("/api/v1/admin/stores/" + storeId + "/menus")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(menuCreateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("수제 크루키"))
                .andExpect(jsonPath("$.data.isSoldOut").value(false))
                .andReturn();

        Long menuId = objectMapper.readTree(menuResult.getResponse().getContentAsString())
                .get("data").get("menuId").asLong();

        // 3. 메뉴 품절 토글 (PATCH /api/v1/admin/stores/{storeId}/menus/{menuId}/sold-out)
        mockMvc.perform(patch("/api/v1/admin/stores/" + storeId + "/menus/" + menuId + "/sold-out")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isSoldOut").value(true)); // 품절 상태 반영

        // 4. 예약 슬롯 개설 (POST /api/v1/admin/stores/{storeId}/slots)
        ReservationSlotCreateRequest slotRequest = ReservationSlotCreateRequest.builder()
                .slotDate(LocalDate.now().plusDays(7))
                .slotTime(LocalTime.of(15, 0))
                .maxCapacity(4)
                .build();

        mockMvc.perform(post("/api/v1/admin/stores/" + storeId + "/slots")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slotRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.storeId").value(storeId))
                .andExpect(jsonPath("$.data.maxCapacity").value(4))
                .andExpect(jsonPath("$.data.isAvailable").value(true));

        // 5. 7% 플랫폼 수수료 및 정산 동적 계산 조회 (GET /api/v1/admin/stores/{storeId}/settlement)
        mockMvc.perform(get("/api/v1/admin/stores/" + storeId + "/settlement")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeId").value(storeId))
                .andExpect(jsonPath("$.data.platformFeeRate").value(7.0))
                .andExpect(jsonPath("$.data.completedOrderCount").value(0))
                .andExpect(jsonPath("$.data.totalOrderAmount").value(0));
    }
}
