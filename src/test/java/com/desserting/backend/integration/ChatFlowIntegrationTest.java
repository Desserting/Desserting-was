package com.desserting.backend.integration;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ChatFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        // 프리미엄 테스트 유저(minjung@desserting.com) 로그인
        LoginRequest loginRequest = LoginRequest.builder()
                .email("minjung@desserting.com")
                .password("test1234!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        userToken = jsonNode.get("data").get("accessToken").asText();
    }

    @Test
    @DisplayName("[통합 E2E] 받은 대화 요청 목록 조회 검증")
    void getReceivedRequests_Success() throws Exception {
        mockMvc.perform(get("/api/v1/matches/requests/received")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("[통합 E2E] 프리미엄 회원 나를 좋아요한 사람 목록 조회 검증 (LIKE-02)")
    void getMyLikesReceived_PremiumSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/likes")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("[통합 E2E] 내 활성 채팅방 목록 및 MongoDB 메시지 커서 페이징 조회 검증 (CHT-01~02)")
    void chatRoomsAndMessages_Success() throws Exception {
        // 1. 내 활성 채팅방 목록 조회 (GET /api/v1/chats/rooms)
        MvcResult roomsResult = mockMvc.perform(get("/api/v1/chats/rooms")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        JsonNode roomsJson = objectMapper.readTree(roomsResult.getResponse().getContentAsString());
        assertThat(roomsJson.get("data").size()).isGreaterThanOrEqualTo(1);

        Long roomId = roomsJson.get("data").get(0).get("roomId").asLong();

        // 2. 해당 방의 MongoDB 이전 메시지 페이징 조회 (GET /api/v1/chats/rooms/{roomId}/messages)
        mockMvc.perform(get("/api/v1/chats/rooms/" + roomId + "/messages")
                        .header("Authorization", "Bearer " + userToken)
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
