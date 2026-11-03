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

    @Transactional
    public ChatRequestResponse sendRequest(Long senderId, ChatRequestCreateRequest request) {
        Long receiverId = request.getReceiverId();

        if (senderId.equals(receiverId)) {
            throw new CustomException(ErrorCode.CANNOT_REQUEST_SELF);
        }

        if (userBlockRepository.isBlockedBetween(senderId, receiverId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "차단 관계인 사용자에게는 대화 요청을 보낼 수 없습니다.");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "대화 요청 대상 사용자를 찾을 수 없습니다: " + receiverId));

        if (chatRoomMemberRepository.existsActiveRoomBetween(senderId, receiverId)) {
            throw new CustomException(ErrorCode.DUPLICATE_CHAT_REQUEST, "이미 활성 채팅방이 존재하는 상대입니다.");
        }

        if (chatRequestRepository.existsBySenderIdAndReceiverIdAndStatus(senderId, receiverId, RequestStatus.PENDING)) {
            throw new CustomException(ErrorCode.DUPLICATE_CHAT_REQUEST, "이미 해당 상대에게 보낸 대기 중인 요청이 존재합니다.");
        }

        // HRT-02: 하트 1개 차감 (프리미엄 회원은 자동 면제)
        heartService.deductHeartForRequest(senderId);

        ChatRequest chatRequest = ChatRequest.create(sender, receiver);
        ChatRequest savedRequest = chatRequestRepository.save(chatRequest);

        log.info("대화 요청 발송 완료: requestId={}, from(userId={}) -> to(userId={})",
                savedRequest.getId(), senderId, receiverId);

        return ChatRequestResponse.from(savedRequest);
    }

    @Transactional(readOnly = true)
    public List<ChatRequestResponse> getReceivedRequests(Long receiverId) {
        List<ChatRequest> requests = chatRequestRepository.findReceivedRequestsWithSender(receiverId, RequestStatus.PENDING);
        return requests.stream()
                .map(ChatRequestResponse::from)
                .toList();
    }

    @Transactional
    public ChatRequestAcceptResponse acceptRequest(Long currentUserId, Long requestId) {
        ChatRequest request = chatRequestRepository.findByIdWithUsers(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_REQUEST_NOT_FOUND));

        if (!request.getReceiver().getId().equals(currentUserId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "본인에게 수신된 대화 요청만 수락할 수 있습니다.");
        }

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

        request.reject();

        // HRT-03: 상대방 거절 시 보낸 사람에게 하트 1개 환불 처리
        heartService.refundHeart(request.getSender().getId(), "상대방 대화 요청 거절 환불");

        log.info("대화 요청 거절 처리: requestId={}, 보낸사람에게 하트 환불 완료", requestId);
    }
}
