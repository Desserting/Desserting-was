package com.desserting.backend.domain.matching.service;

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
import com.desserting.backend.domain.matching.repository.MatchExposureRepository;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.repository.UserBlockRepository;
import com.desserting.backend.domain.user.repository.UserRepository;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRequestService {

    private final ChatRequestRepository chatRequestRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final HeartService heartService;
    private final UserBlockRepository userBlockRepository;
    private final MatchExposureRepository matchExposureRepository;

    @Transactional
    public ChatRequestResponse sendRequest(Long senderId, ChatRequestCreateRequest request) {
        Long receiverId = request.getReceiverId();

        if (senderId.equals(receiverId)) {
            throw new CustomException(ErrorCode.CANNOT_REQUEST_SELF);
        }

        if (userBlockRepository.isBlockedBetween(senderId, receiverId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "차단 관계인 사용자에게는 대화 요청을 보낼 수 없습니다.");
        }

        if (chatRoomMemberRepository.existsActiveRoomBetween(senderId, receiverId)) {
            throw new CustomException(ErrorCode.DUPLICATE_CHAT_REQUEST, "이미 활성 채팅방이 존재하는 상대입니다.");
        }

        if (chatRequestRepository.existsBySenderIdAndReceiverIdAndStatus(senderId, receiverId, RequestStatus.PENDING)) {
            throw new CustomException(ErrorCode.DUPLICATE_CHAT_REQUEST, "이미 해당 상대에게 보낸 대기 중인 요청이 존재합니다.");
        }

        // HRT-02: 하트 1개 차감 (동시성 lost update 방지를 위해 비관적 락으로 먼저 차감 및 최신 sender 획득)
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "대화 요청 대상 사용자를 찾을 수 없습니다: " + receiverId));
        User sender = heartService.deductHeartForRequest(senderId, receiver.getNickname());
        // MAT-04/PRE-03: 원하는 프로필에 직접 요청은 프리미엄 전용. 일반 회원은 최근 24시간 매칭으로 만난 상대에게만 요청한다
        // (예외 시 트랜잭션 롤백으로 하트 차감도 취소된다)
        if (!Boolean.TRUE.equals(sender.getIsPremium()) && !matchExposureRepository.wasShownRecently(senderId, receiverId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED,
                    "원하는 상대에게 직접 대화 요청하기는 프리미엄 패스 회원 전용 기능입니다. 랜덤 매칭으로 만난 상대에게 요청해 주세요.");
        }
        ChatRequest chatRequest = ChatRequest.create(sender, receiver);
        ChatRequest savedRequest = chatRequestRepository.save(chatRequest);

        log.info("대화 요청 발송 완료: requestId={}, from(userId={}) -> to(userId={})",
                savedRequest.getId(), senderId, receiverId);

        return ChatRequestResponse.from(savedRequest);
    }

    @Transactional(readOnly = true)
    public List<ChatRequestResponse> getReceivedRequests(Long receiverId) {
        return getReceivedRequests(receiverId, false);
    }

    /**
     * includeProcessed=true 면 최근 7일의 수락·거절·만료 요청도 함께 준다.
     * 수락된 요청에는 지금 열려 있는 채팅방 id(chatRoomId)를 채운다 (새로고침 후 "채팅방 가기"용).
     */
    @Transactional(readOnly = true)
    public List<ChatRequestResponse> getReceivedRequests(Long receiverId, boolean includeProcessed) {
        List<ChatRequest> requests = includeProcessed
                ? chatRequestRepository.findRecentReceivedWithSender(receiverId, java.time.LocalDateTime.now().minusDays(7))
                : chatRequestRepository.findReceivedRequestsWithSender(receiverId, RequestStatus.PENDING);
        return requests.stream()
                .map(r -> {
                    ChatRequestResponse res = ChatRequestResponse.from(r);
                    if (r.getStatus() == RequestStatus.ACCEPTED) {
                        chatRoomMemberRepository.findActiveRoomsBetween(receiverId, r.getSender().getId()).stream()
                                .findFirst()
                                .ifPresent(m -> res.assignChatRoomId(m.getChatRoom().getId()));
                    }
                    return res;
                })
                .toList();
    }

    @Transactional
    public ChatRequestAcceptResponse acceptRequest(Long currentUserId, Long requestId) {
        ChatRequest request = chatRequestRepository.findByIdWithUsers(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_REQUEST_NOT_FOUND));

        if (!request.getReceiver().getId().equals(currentUserId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "본인에게 수신된 대화 요청만 수락할 수 있습니다.");
        }
        ensurePending(request);

        request.accept();

        // CHT-01, REQ-01: 요청 수락 시 1:1 채팅방 생성 및 양측 참여자 등록
        ChatRoom chatRoom = ChatRoom.create();
        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);

        chatRoomMemberRepository.save(ChatRoomMember.of(savedRoom, request.getSender()));
        chatRoomMemberRepository.save(ChatRoomMember.of(savedRoom, request.getReceiver()));

        log.info("대화 요청 수락 완료: requestId={}, 신규 채팅방 개설 roomId={}, roomUuid={}",
                requestId, savedRoom.getId(), savedRoom.getRoomUuid());

        return ChatRequestAcceptResponse.builder()
                .requestId(requestId)
                .chatRoomId(savedRoom.getId())
                .roomUuid(savedRoom.getRoomUuid())
                .message("대화 요청이 수락되어 채팅방이 개설되었습니다.")
                .build();
    }

    @Transactional
    public void rejectRequest(Long currentUserId, Long requestId) {
        ChatRequest request = chatRequestRepository.findByIdWithUsers(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_REQUEST_NOT_FOUND));

        if (!request.getReceiver().getId().equals(currentUserId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "본인에게 수신된 대화 요청만 거절할 수 있습니다.");
        }
        ensurePending(request);

        request.reject();

        // HRT-03: 상대방 거절 시 보낸 사람에게 하트 1개 환불 처리
        heartService.refundHeart(request.getSender().getId(),
                request.getReceiver().getNickname() + "님이 대화 요청을 거절해 하트 환불");

        log.info("대화 요청 거절 처리: requestId={}, 보낸사람에게 하트 환불 완료", requestId);
    }

    @Transactional
    public int expireStaleRequests() {
        List<ChatRequest> staleRequests = chatRequestRepository.findAllByStatusAndExpiredAtLessThanEqual(
                RequestStatus.PENDING, java.time.LocalDateTime.now()
        );
        for (ChatRequest req : staleRequests) {
            req.expire();
            heartService.refundHeart(req.getSender().getId(),
                    req.getReceiver().getNickname() + "님이 24시간 동안 응답하지 않아 하트 환불");
        }
        if (!staleRequests.isEmpty()) {
            log.info("24시간 미응답 대화 요청 자동 만료 및 환불 완료: {}건", staleRequests.size());
        }
        return staleRequests.size();
    }

    /** 이미 처리됐거나 만료된 요청을 다시 수락·거절하면 500 대신 409(H005)/410(H006) */
    private void ensurePending(ChatRequest request) {
        if (request.getStatus() == RequestStatus.EXPIRED
                || (request.getStatus() == RequestStatus.PENDING && request.isExpired())) {
            throw new CustomException(ErrorCode.CHAT_REQUEST_EXPIRED);
        }
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new CustomException(ErrorCode.CHAT_REQUEST_ALREADY_PROCESSED);
        }
    }
}
