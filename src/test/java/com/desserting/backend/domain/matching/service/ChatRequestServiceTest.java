package com.desserting.backend.domain.matching.service;

import static org.mockito.Mockito.never;
import com.desserting.backend.domain.matching.repository.MatchExposureRepository;
import com.desserting.backend.domain.chat.entity.ChatRoom;
import com.desserting.backend.domain.chat.entity.ChatRoomMember;
import com.desserting.backend.domain.chat.repository.ChatRoomMemberRepository;
import com.desserting.backend.domain.chat.repository.ChatRoomRepository;
import com.desserting.backend.domain.heart.service.HeartService;
import com.desserting.backend.domain.matching.dto.ChatRequestAcceptResponse;
import com.desserting.backend.domain.matching.dto.ChatRequestCreateRequest;
import com.desserting.backend.domain.matching.dto.ChatRequestResponse;
import com.desserting.backend.domain.matching.entity.ChatRequest;
import com.desserting.backend.domain.matching.entity.RequestStatus;
import com.desserting.backend.domain.matching.repository.ChatRequestRepository;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.repository.UserBlockRepository;
import com.desserting.backend.domain.user.repository.UserRepository;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatRequestServiceTest {

    @Mock
    private ChatRequestRepository chatRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private HeartService heartService;

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private MatchExposureRepository matchExposureRepository;

    @InjectMocks
    private ChatRequestService chatRequestService;

    @Test
    @DisplayName("정상 대화 요청 발송 시 하트가 차감되고 PENDING 요청이 생성된다")
    void sendRequest_Success() {
        // given
        User sender = User.builder().email("sender@desserting.com").nickname("보낸사람").dessertTags(new ArrayList<>()).build();
        ReflectionTestUtils.setField(sender, "id", 1L);

        User receiver = User.builder().email("receiver@desserting.com").nickname("받는사람").dessertTags(new ArrayList<>()).build();
        ReflectionTestUtils.setField(receiver, "id", 2L);

        given(userRepository.findById(1L)).willReturn(Optional.of(sender));
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(heartService.deductHeartForRequest(eq(1L), any())).willReturn(sender);
        given(matchExposureRepository.wasShownRecently(1L, 2L)).willReturn(true);
        given(chatRoomMemberRepository.existsActiveRoomBetween(1L, 2L)).willReturn(false);
        given(chatRequestRepository.existsBySenderIdAndReceiverIdAndStatus(1L, 2L, RequestStatus.PENDING)).willReturn(false);

        given(chatRequestRepository.save(any(ChatRequest.class))).willAnswer(invocation -> {
            ChatRequest req = invocation.getArgument(0);
            ReflectionTestUtils.setField(req, "id", 100L);
            ReflectionTestUtils.setField(req, "createdAt", LocalDateTime.now());
            return req;
        });

        // when
        ChatRequestResponse response = chatRequestService.sendRequest(1L, new ChatRequestCreateRequest(2L));

        // then
        assertThat(response).isNotNull();
        assertThat(response.getRequestId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(RequestStatus.PENDING);
        verify(heartService).deductHeartForRequest(1L, "받는사람");
    }

    @Test
    @DisplayName("일반 회원이 매칭으로 만나지 않은 상대에게 직접 요청하면 하트 차감 전 ACCESS_DENIED (Fail-Fast)")
    void sendRequest_DirectRequestRequiresPremium() {
        User sender = User.builder().nickname("일반").isPremium(false).dessertTags(new ArrayList<>()).build();
        ReflectionTestUtils.setField(sender, "id", 1L);
        User target = User.builder().nickname("받는사람").dessertTags(new ArrayList<>()).build();
        ReflectionTestUtils.setField(target, "id", 2L);

        given(userRepository.findById(1L)).willReturn(Optional.of(sender));
        given(userRepository.findById(2L)).willReturn(Optional.of(target));
        given(matchExposureRepository.wasShownRecently(1L, 2L)).willReturn(false);

        assertThatThrownBy(() -> chatRequestService.sendRequest(1L, new ChatRequestCreateRequest(2L)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
        verify(heartService, never()).deductHeartForRequest(anyLong(), any());
        verify(chatRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("프리미엄 회원은 매칭 이력 없이도 원하는 상대에게 직접 요청할 수 있다")
    void sendRequest_PremiumDirectRequest() {
        User sender = User.builder().nickname("프리미엄").isPremium(true).dessertTags(new ArrayList<>()).build();
        ReflectionTestUtils.setField(sender, "id", 1L);
        User receiver = User.builder().nickname("받는사람").dessertTags(new ArrayList<>()).build();
        ReflectionTestUtils.setField(receiver, "id", 2L);

        given(userRepository.findById(1L)).willReturn(Optional.of(sender));
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(heartService.deductHeartForRequest(eq(1L), any())).willReturn(sender);
        given(chatRequestRepository.save(any(ChatRequest.class))).willAnswer(inv -> {
            ChatRequest req = inv.getArgument(0);
            ReflectionTestUtils.setField(req, "createdAt", LocalDateTime.now());
            return req;
        });

        assertThat(chatRequestService.sendRequest(1L, new ChatRequestCreateRequest(2L)).getStatus())
                .isEqualTo(RequestStatus.PENDING);
        verify(matchExposureRepository, never()).wasShownRecently(any(), any());
    }

    @Test
    @DisplayName("자신에게 대화 요청 시 CANNOT_REQUEST_SELF 예외 발생")
    void sendRequest_SelfRequest() {
        // when & then
        assertThatThrownBy(() -> chatRequestService.sendRequest(1L, new ChatRequestCreateRequest(1L)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_REQUEST_SELF);
    }

    @Test
    @DisplayName("차단 관계인 상대방에게 대화 요청 발송 시 ACCESS_DENIED 예외 발생")
    void sendRequest_BlockedUser() {
        // given
        given(userBlockRepository.isBlockedBetween(1L, 2L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> chatRequestService.sendRequest(1L, new ChatRequestCreateRequest(2L)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("이미 대기 중인 요청이 존재할 때 중복 요청 시 DUPLICATE_CHAT_REQUEST 예외 발생")
    void sendRequest_DuplicatePending() {
        // given
        given(chatRoomMemberRepository.existsActiveRoomBetween(1L, 2L)).willReturn(false);
        given(chatRequestRepository.existsBySenderIdAndReceiverIdAndStatus(1L, 2L, RequestStatus.PENDING)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> chatRequestService.sendRequest(1L, new ChatRequestCreateRequest(2L)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_CHAT_REQUEST);
    }

    @Test
    @DisplayName("대화 요청 수락 시 요청 상태가 ACCEPTED로 변경되고 ChatRoom 및 2명의 멤버가 생성된다")
    void acceptRequest_Success() {
        // given
        User sender = User.builder().email("sender@desserting.com").nickname("보낸사람").build();
        ReflectionTestUtils.setField(sender, "id", 1L);

        User receiver = User.builder().email("receiver@desserting.com").nickname("받는사람").build();
        ReflectionTestUtils.setField(receiver, "id", 2L);

        ChatRequest request = ChatRequest.create(sender, receiver);
        ReflectionTestUtils.setField(request, "id", 10L);

        ChatRoom chatRoom = ChatRoom.create();
        ReflectionTestUtils.setField(chatRoom, "id", 50L);

        given(chatRequestRepository.findByIdWithUsers(10L)).willReturn(Optional.of(request));
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(chatRoom);

        // when (receiver = 2L가 수락)
        ChatRequestAcceptResponse response = chatRequestService.acceptRequest(2L, 10L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getChatRoomId()).isEqualTo(50L);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.ACCEPTED);
        verify(chatRoomMemberRepository, org.mockito.Mockito.times(2)).save(any(ChatRoomMember.class));
    }

    @Test
    @DisplayName("다른 사람의 대화 요청을 수락 시도 시 ACCESS_DENIED 예외 발생")
    void acceptRequest_AccessDenied() {
        // given
        User sender = User.builder().build();
        ReflectionTestUtils.setField(sender, "id", 1L);
        User receiver = User.builder().build();
        ReflectionTestUtils.setField(receiver, "id", 2L);

        ChatRequest request = ChatRequest.create(sender, receiver);
        given(chatRequestRepository.findByIdWithUsers(10L)).willReturn(Optional.of(request));

        // when & then (제3자 99L가 수락 시도)
        assertThatThrownBy(() -> chatRequestService.acceptRequest(99L, 10L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("대화 요청 거절 시 요청 상태가 REJECTED로 변경되고 하트가 환불된다")
    void rejectRequest_Success() {
        // given
        User sender = User.builder().build();
        ReflectionTestUtils.setField(sender, "id", 1L);
        User receiver = User.builder().nickname("받는사람").build();
        ReflectionTestUtils.setField(receiver, "id", 2L);

        ChatRequest request = ChatRequest.create(sender, receiver);
        ReflectionTestUtils.setField(request, "id", 20L);

        given(chatRequestRepository.findByIdWithUsers(20L)).willReturn(Optional.of(request));

        // when
        chatRequestService.rejectRequest(2L, 20L);

        // then
        assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
        verify(heartService).refundHeart(1L, "받는사람님이 대화 요청을 거절해 하트 환불");
    }

    @Test
    @DisplayName("24시간이 경과한 PENDING 대화 요청은 자동으로 EXPIRED 처리되고 하트가 100% 환불된다")
    void expireStaleRequests_ExpiresAndRefunds() {
        // given
        User sender = User.builder().build();
        ReflectionTestUtils.setField(sender, "id", 1L);
        User receiver = User.builder().nickname("받는사람").build();
        ReflectionTestUtils.setField(receiver, "id", 2L);

        ChatRequest staleRequest = ChatRequest.create(sender, receiver);
        ReflectionTestUtils.setField(staleRequest, "id", 30L);
        ReflectionTestUtils.setField(staleRequest, "expiredAt", LocalDateTime.now().minusHours(1));

        given(chatRequestRepository.findAllByStatusAndExpiredAtLessThanEqual(eq(RequestStatus.PENDING), any(LocalDateTime.class)))
                .willReturn(List.of(staleRequest));

        // when
        int count = chatRequestService.expireStaleRequests();

        // then
        assertThat(count).isEqualTo(1);
        assertThat(staleRequest.getStatus()).isEqualTo(RequestStatus.EXPIRED);
        verify(heartService).refundHeart(1L, "받는사람님이 24시간 동안 응답하지 않아 하트 환불");
    }

    @Test
    @DisplayName("만료된 대화 요청을 수락하려 하면 500 대신 CHAT_REQUEST_EXPIRED(H006)")
    void acceptRequest_ThrowsException_WhenExpired() {
        // given
        User sender = User.builder().build();
        ReflectionTestUtils.setField(sender, "id", 1L);
        User receiver = User.builder().build();
        ReflectionTestUtils.setField(receiver, "id", 2L);

        ChatRequest expiredRequest = ChatRequest.create(sender, receiver);
        ReflectionTestUtils.setField(expiredRequest, "id", 40L);
        ReflectionTestUtils.setField(expiredRequest, "expiredAt", LocalDateTime.now().minusHours(2));

        given(chatRequestRepository.findByIdWithUsers(40L)).willReturn(Optional.of(expiredRequest));

        // when & then
        assertThatThrownBy(() -> chatRequestService.acceptRequest(2L, 40L))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_REQUEST_EXPIRED);
    }

    @Test
    @DisplayName("이미 수락·거절된 요청을 다시 수락·거절하면 500 대신 CHAT_REQUEST_ALREADY_PROCESSED(H005)")
    void acceptOrReject_AlreadyProcessed() {
        User sender = User.builder().build();
        ReflectionTestUtils.setField(sender, "id", 1L);
        User receiver = User.builder().build();
        ReflectionTestUtils.setField(receiver, "id", 2L);
        ChatRequest accepted = ChatRequest.create(sender, receiver);
        accepted.accept();
        given(chatRequestRepository.findByIdWithUsers(50L)).willReturn(Optional.of(accepted));

        assertThatThrownBy(() -> chatRequestService.acceptRequest(2L, 50L))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_REQUEST_ALREADY_PROCESSED);
        assertThatThrownBy(() -> chatRequestService.rejectRequest(2L, 50L))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_REQUEST_ALREADY_PROCESSED);
        verify(heartService, never()).refundHeart(any(), any());
    }

    @Test
    @DisplayName("includeProcessed=true 면 수락된 요청도 내려오고 열린 채팅방 id 가 채워진다")
    void receivedRequests_IncludeProcessed() {
        User sender = User.builder().nickname("보낸사람").dessertTags(new ArrayList<>()).build();
        ReflectionTestUtils.setField(sender, "id", 1L);
        User receiver = User.builder().build();
        ReflectionTestUtils.setField(receiver, "id", 2L);
        ChatRequest accepted = ChatRequest.create(sender, receiver);
        accepted.accept();
        ChatRoom room = ChatRoom.create();
        ReflectionTestUtils.setField(room, "id", 77L);
        given(chatRequestRepository.findRecentReceivedWithSender(eq(2L), any(LocalDateTime.class))).willReturn(List.of(accepted));
        given(chatRoomMemberRepository.findActiveRoomsBetween(2L, 1L))
                .willReturn(List.of(ChatRoomMember.builder().chatRoom(room).user(receiver).isLeft(false).build()));

        List<ChatRequestResponse> result = chatRequestService.getReceivedRequests(2L, true);

        assertThat(result).singleElement().satisfies(r -> {
            assertThat(r.getStatus()).isEqualTo(RequestStatus.ACCEPTED);
            assertThat(r.getChatRoomId()).isEqualTo(77L);
        });
    }
}
