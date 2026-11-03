package com.desserting.backend.domain.ai.service;

import com.desserting.backend.domain.ai.client.OllamaClient;
import com.desserting.backend.domain.ai.dto.AiRecommendationResponse;
import com.desserting.backend.domain.chat.document.ChatMessageDocument;
import com.desserting.backend.domain.chat.repository.ChatMessageRepository;
import com.desserting.backend.domain.chat.repository.ChatRoomMemberRepository;
import com.desserting.backend.domain.chat.service.ChatService;
import com.desserting.backend.domain.store.entity.Store;
import com.desserting.backend.domain.store.entity.StoreMenu;
import com.desserting.backend.domain.store.repository.StoreMenuRepository;
import com.desserting.backend.domain.store.repository.StoreRepository;
import com.desserting.backend.domain.user.repository.UserRepository;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiRecommendationServiceTest {

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private StoreMenuRepository storeMenuRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ChatService chatService;
    @Mock
    private OllamaClient ollamaClient;

    private AiRecommendationService service;
    private Store storeLucid;
    private Store storeAnook;

    @BeforeEach
    void setUp() {
        service = new AiRecommendationService(
                chatRoomMemberRepository,
                chatMessageRepository,
                storeRepository,
                storeMenuRepository,
                userRepository,
                chatService,
                ollamaClient,
                new ObjectMapper()
        );

        storeLucid = Store.builder().name("카페 루시드").address("대구 중구").isPartner(true).build();
        ReflectionTestUtils.setField(storeLucid, "id", 1L);

        storeAnook = Store.builder().name("아눅 앞산").address("대구 남구").isPartner(true).build();
        ReflectionTestUtils.setField(storeAnook, "id", 2L);
    }

    @Test
    @DisplayName("Ollama 로컬 LLM이 JSON 응답을 정상 반환하면 매장과 메뉴를 파싱하여 채팅방에 카드를 전송한다")
    void recommendForRoom_OllamaSuccess() {
        // given
        Long roomId = 10L;
        Long userId = 1L;

        given(chatRoomMemberRepository.existsByChatRoomIdAndUserIdAndIsLeftFalse(roomId, userId)).willReturn(true);
        given(storeRepository.findAllByIsPartnerTrue()).willReturn(List.of(storeLucid, storeAnook));
        given(chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(eq(roomId), any())).willReturn(List.of());

        String jsonResponse = """
                {
                  "storeId": 2,
                  "reason": "앞산 전경과 크루키가 유명한 아눅 앞산을 추천합니다.",
                  "recommendedMenus": ["크루키", "아메리카노"]
                }
                """;
        given(ollamaClient.generate(anyString(), anyString())).willReturn(Optional.of(jsonResponse));

        // when
        AiRecommendationResponse response = service.recommendForRoom(roomId, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.storeId()).isEqualTo(2L);
        assertThat(response.storeName()).isEqualTo("아눅 앞산");
        assertThat(response.recommendedMenus()).containsExactly("크루키", "아메리카노");
        assertThat(response.source()).isEqualTo("OLLAMA_LOCAL_QWEN");
        verify(chatService).sendMessage(eq(userId), any());
    }

    @Test
    @DisplayName("Ollama가 미기동 상태이거나 타임아웃일 때 대화 키워드를 분석하는 스마트 Fallback이 정상 작동한다")
    void recommendForRoom_SmartFallbackSuccess() {
        // given
        Long roomId = 10L;
        Long userId = 1L;

        given(chatRoomMemberRepository.existsByChatRoomIdAndUserIdAndIsLeftFalse(roomId, userId)).willReturn(true);
        given(storeRepository.findAllByIsPartnerTrue()).willReturn(List.of(storeLucid, storeAnook));

        ChatMessageDocument doc = ChatMessageDocument.builder()
                .senderNickname("유저1")
                .content("우리 소금빵 맛있는 데 가요!")
                .build();
        given(chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(eq(roomId), any())).willReturn(List.of(doc));

        StoreMenu menu = StoreMenu.builder().name("소금빵").price(3500).build();
        given(storeMenuRepository.findAllByStoreIdAndIsSoldOutFalse(1L)).willReturn(List.of(menu));
        given(storeMenuRepository.findAllByStoreIdAndIsSoldOutFalse(2L)).willReturn(List.of());

        // Ollama는 실패
        given(ollamaClient.generate(anyString(), anyString())).willReturn(Optional.empty());

        // when
        AiRecommendationResponse response = service.recommendForRoom(roomId, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.storeId()).isEqualTo(1L);
        assertThat(response.storeName()).isEqualTo("카페 루시드");
        assertThat(response.recommendedMenus()).contains("소금빵");
        assertThat(response.source()).isEqualTo("SMART_KEYWORD_FALLBACK");
        verify(chatService).sendMessage(eq(userId), any());
    }

    @Test
    @DisplayName("채팅방 멤버가 아닌 유저가 추천을 요청하면 CHAT_MEMBER_NOT_ALLOWED 예외 발생")
    void recommendForRoom_NotMember_ThrowsException() {
        given(chatRoomMemberRepository.existsByChatRoomIdAndUserIdAndIsLeftFalse(10L, 99L)).willReturn(false);

        assertThatThrownBy(() -> service.recommendForRoom(10L, 99L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_MEMBER_NOT_ALLOWED);
    }
}
