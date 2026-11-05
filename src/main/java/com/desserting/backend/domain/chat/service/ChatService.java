package com.desserting.backend.domain.chat.service;

import com.desserting.backend.domain.chat.document.ChatMessageDocument;
import com.desserting.backend.domain.chat.dto.ChatMessageDto;
import com.desserting.backend.domain.chat.dto.ChatMessageRequest;
import com.desserting.backend.domain.chat.dto.ChatRoomResponse;
import com.desserting.backend.domain.chat.entity.ChatRoom;
import com.desserting.backend.domain.chat.entity.ChatRoomMember;
import com.desserting.backend.domain.chat.redis.RedisPublisher;
import com.desserting.backend.domain.chat.repository.ChatMessageQueryRepository;
import com.desserting.backend.domain.chat.repository.ChatMessageRepository;
import com.desserting.backend.domain.chat.repository.ChatRoomMemberRepository;
import com.desserting.backend.domain.chat.repository.ChatRoomRepository;
import com.desserting.backend.domain.matching.entity.RequestStatus;
import com.desserting.backend.domain.matching.repository.ChatRequestRepository;
import com.desserting.backend.domain.chat.dto.PlaceProposalRequest;
import com.desserting.backend.domain.chat.dto.ShareReservationRequest;
import com.desserting.backend.domain.order.entity.ReservationOrder;
import com.desserting.backend.domain.order.repository.ReservationOrderRepository;
import com.desserting.backend.domain.reservation.entity.Reservation;
import com.desserting.backend.domain.reservation.repository.UserReservationRepository;
import com.desserting.backend.domain.store.entity.Store;
import com.desserting.backend.domain.store.repository.StoreRepository;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.entity.UserRole;
import com.desserting.backend.domain.user.repository.UserBlockRepository;
import com.desserting.backend.domain.user.repository.UserRepository;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final RedisPublisher redisPublisher;
    private final UserBlockRepository userBlockRepository;
    private final ChatMessageQueryRepository chatMessageQueryRepository;
    private final ChatRequestRepository chatRequestRepository;
    private final StoreRepository storeRepository;
    private final UserReservationRepository userReservationRepository;
    private final ReservationOrderRepository reservationOrderRepository;

    public static final String PLACE_PROPOSAL_TYPE = "PLACE_PROPOSAL";
    public static final String RESERVATION_CARD_TYPE = "RESERVATION_CARD";

    private static final String SYSTEM_TYPE = "SYSTEM";

    @Transactional
    public ChatMessageDto sendMessage(Long senderId, ChatMessageRequest request) {
        Long roomId = request.getRoomId();

        ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, senderId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_MEMBER_NOT_ALLOWED));

        if (Boolean.TRUE.equals(member.getIsLeft())) {
            throw new CustomException(ErrorCode.CHAT_MEMBER_NOT_ALLOWED, "이미 퇴장한 채팅방입니다.");
        }

        // SYSTEM 메시지는 서버만 만든다 (퇴장 안내 등)
        if (SYSTEM_TYPE.equalsIgnoreCase(request.getMessageType())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "SYSTEM 메시지는 보낼 수 없습니다.");
        }

        // 차단 관계 검증: 상대방과의 차단 상태 확인
        Optional<ChatRoomMember> otherMemberOpt = chatRoomMemberRepository.findOtherMember(roomId, senderId);
        if (otherMemberOpt.isPresent() && Boolean.TRUE.equals(otherMemberOpt.get().getIsLeft())) {
            throw new CustomException(ErrorCode.CHAT_MEMBER_NOT_ALLOWED, "상대방이 나간 채팅방입니다.");
        }
        if (otherMemberOpt.isPresent()) {
            Long otherUserId = otherMemberOpt.get().getUser().getId();
            if (userBlockRepository.isBlockedBetween(senderId, otherUserId)) {
                throw new CustomException(ErrorCode.ACCESS_DENIED, "차단 관계인 사용자가 포함된 대화방에서는 메시지를 전송할 수 없습니다.");
            }
        }

        User sender = member.getUser();
        ChatRoom chatRoom = member.getChatRoom();

        // 1. MongoDB에 대용량 채팅 메시지 도큐먼트 영구 적재
        ChatMessageDocument messageDoc = ChatMessageDocument.builder()
                .roomId(roomId)
                .senderId(sender.getId())
                .senderNickname(sender.getNickname())
                .messageType(request.getMessageType())
                .content(request.getContent())
                .metadata(request.getMetadata())
                .readUserIds(new ArrayList<>(Collections.singletonList(sender.getId())))
                .createdAt(LocalDateTime.now())
                .build();

        ChatMessageDocument savedDoc = chatMessageRepository.save(messageDoc);

        // 2. PostgreSQL ChatRoom 메타데이터(마지막 메시지 미리보기/시각) 업데이트
        chatRoom.updateLastMessage(request.getContent());

        // 3. Redis Pub/Sub 메시지 브로드캐스팅
        ChatMessageDto messageDto = ChatMessageDto.from(savedDoc, sender.getDessertAvatarUrl());
        redisPublisher.publish(ChannelTopic.of("chat:room:" + roomId), messageDto);

        log.info("채팅 메시지 전송 성공: roomId={}, senderId={}, messageId={}", roomId, senderId, savedDoc.getId());
        return messageDto;
    }

    /**
     * 데이트 제휴 매장 방문 제안 (PLACE_PROPOSAL) 카드 발송
     */
    @Transactional
    public ChatMessageDto proposePlace(Long senderId, Long roomId, PlaceProposalRequest request) {
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        String customMsg = request.getMessage() != null && !request.getMessage().isBlank()
                ? request.getMessage().trim()
                : store.getName() + " 방문 어떠세요?";

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("storeId", store.getId());
        metadata.put("storeName", store.getName());
        metadata.put("storeAddress", store.getAddress());
        metadata.put("themeStreetName", store.getThemeStreet() != null ? store.getThemeStreet().getName() : null);
        metadata.put("coverImageUrl", store.getCoverImageUrl());

        ChatMessageRequest messageRequest = ChatMessageRequest.builder()
                .roomId(roomId)
                .messageType(PLACE_PROPOSAL_TYPE)
                .content(customMsg)
                .metadata(metadata)
                .build();

        return sendMessage(senderId, messageRequest);
    }

    /**
     * 예약 및 결제 완료 내역 (RESERVATION_CARD) 채팅방 공유
     */
    @Transactional
    public ChatMessageDto shareReservation(Long senderId, Long roomId, ShareReservationRequest request) {
        Reservation reservation = userReservationRepository.findByIdWithStore(request.getReservationId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_NOT_FOUND));

        if (!reservation.getUser().getId().equals(senderId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "본인이 생성한 예약만 채팅방에 공유할 수 있습니다.");
        }

        // 예약 객체에 해당 채팅방 연계 저장
        reservation.updateChatRoomId(roomId);

        Store store = reservation.getStore();
        Optional<ReservationOrder> orderOpt = reservationOrderRepository.findDetailByReservationId(reservation.getId());

        String defaultContent = "[" + store.getName() + "] 예약 및 결제가 완료되었습니다!";
        String content = request.getMessage() != null && !request.getMessage().isBlank()
                ? request.getMessage().trim()
                : defaultContent;

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reservationId", reservation.getId());
        metadata.put("storeId", store.getId());
        metadata.put("storeName", store.getName());
        metadata.put("storeAddress", store.getAddress());
        metadata.put("reservationDate", reservation.getReservationDate().toString());
        metadata.put("reservationTime", reservation.getReservationTime().toString());
        metadata.put("guestCount", reservation.getGuestCount());
        metadata.put("status", reservation.getStatus().name());
        metadata.put("statusDescription", reservation.getStatus().getDescription());
        metadata.put("totalAmount", reservation.getTotalAmount());
        metadata.put("discountAmount", reservation.getDiscountAmount());
        metadata.put("finalAmount", reservation.getFinalAmount());

        orderOpt.ifPresent(order -> {
            metadata.put("orderId", order.getId());
            metadata.put("paymentMethod", order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
            if (order.getItems() != null) {
                List<Map<String, Object>> items = order.getItems().stream().map(item -> {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("menuName", item.getMenuName());
                    itemMap.put("unitPrice", item.getUnitPrice());
                    itemMap.put("quantity", item.getQuantity());
                    itemMap.put("lineAmount", item.getLineAmount());
                    return itemMap;
                }).toList();
                metadata.put("orderedItems", items);
            }
        });

        ChatMessageRequest messageRequest = ChatMessageRequest.builder()
                .roomId(roomId)
                .messageType(RESERVATION_CARD_TYPE)
                .content(content)
                .metadata(metadata)
                .build();

        return sendMessage(senderId, messageRequest);
    }


    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyChatRooms(Long userId) {
        List<ChatRoomMember> memberships = chatRoomMemberRepository.findAllMyActiveMemberships(userId);
        List<ChatRoomResponse> responses = new ArrayList<>();

        for (ChatRoomMember member : memberships) {
            ChatRoom room = member.getChatRoom();
            Optional<ChatRoomMember> otherMemberOpt = chatRoomMemberRepository.findOtherMember(room.getId(), userId);

            String otherNickname = "알 수 없음";
            String otherAvatarUrl = null;
            Long otherUserId = null;
            boolean otherUserLeft = otherMemberOpt.map(m -> Boolean.TRUE.equals(m.getIsLeft())).orElse(false);

            if (otherMemberOpt.isPresent()) {
                User otherUser = otherMemberOpt.get().getUser();
                otherNickname = otherUser.getNickname();
                otherAvatarUrl = otherUser.getDessertAvatarUrl();
                otherUserId = otherUser.getId();
            }

            responses.add(ChatRoomResponse.builder()
                    .roomId(room.getId())
                    .roomUuid(room.getRoomUuid())
                    .otherUserId(otherUserId)
                    .otherUserNickname(otherNickname)
                    .otherUserAvatarUrl(otherAvatarUrl)
                    .lastMessagePreview(room.getLastMessagePreview())
                    .lastMessageAt(room.getLastMessageAt())
                    .unreadCount(chatMessageQueryRepository.countUnread(room.getId(), userId))
                    .otherUserLeft(otherUserLeft)
                    .build());
        }

        return responses;
    }

    @Transactional
    public List<ChatMessageDto> getChatMessages(Long userId, Long roomId, String cursorId, int size) {
        ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_MEMBER_NOT_ALLOWED));

        if (Boolean.TRUE.equals(member.getIsLeft())) {
            throw new CustomException(ErrorCode.CHAT_MEMBER_NOT_ALLOWED);
        }

        List<ChatMessageDocument> documents =
                chatMessageQueryRepository.findPage(roomId, cursorId, Math.max(1, Math.min(size, 100)));

        // 방에 들어와 최신 페이지를 열면 안 읽은 메시지를 모두 읽음 처리
        if (cursorId == null || cursorId.isBlank()) {
            chatMessageQueryRepository.markAllRead(roomId, userId);
        }

        // DTO 변환 (발신자 아바타 일괄 조회로 N+1 DB 쿼리 제거)
        Set<Long> senderIds = documents.stream()
                .map(ChatMessageDocument::getSenderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> avatarMap = new HashMap<>();
        if (!senderIds.isEmpty()) {
            for (User sender : userRepository.findAllById(senderIds)) {
                avatarMap.put(sender.getId(), sender.getDessertAvatarUrl());
            }
        }

        return documents.stream()
                .map(doc -> {
                    String avatarUrl = doc.getSenderId() != null ? avatarMap.get(doc.getSenderId()) : null;
                    return ChatMessageDto.from(doc, avatarUrl);
                })
                .toList();
    }

    /** 채팅방을 보고 있는 동안 실시간으로 받은 메시지를 읽음 처리 */
    @Transactional(readOnly = true)
    public void markAsRead(Long userId, Long roomId) {
        if (!chatRoomMemberRepository.existsByChatRoomIdAndUserIdAndIsLeftFalse(roomId, userId)) {
            throw new CustomException(ErrorCode.CHAT_MEMBER_NOT_ALLOWED);
        }
        chatMessageQueryRepository.markAllRead(roomId, userId);
    }

    @Transactional
    public void leaveChatRoom(Long userId, Long roomId) {
        ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_MEMBER_NOT_ALLOWED));
        if (Boolean.TRUE.equals(member.getIsLeft())) {
            return; // 이미 나간 방
        }

        // Soft delete: isLeft = true
        // Entity update via dirty checking or method
        member = ChatRoomMember.builder()
                .id(member.getId())
                .chatRoom(member.getChatRoom())
                .user(member.getUser())
                .lastReadAt(member.getLastReadAt())
                .isLeft(true)
                .build();
        chatRoomMemberRepository.save(member);
        publishLeaveNotice(member);
        log.info("채팅방 퇴장 완료: userId={}, roomId={}", userId, roomId);
    }

    @Transactional
    public ChatRoomResponse getOrCreateDirectRoom(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new CustomException(ErrorCode.CANNOT_REQUEST_SELF, "자기 자신과는 대화할 수 없습니다.");
        }

        if (userBlockRepository.isBlockedBetween(currentUserId, targetUserId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "차단 관계인 사용자와는 대화방을 개설할 수 없습니다.");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "현재 사용자를 찾을 수 없습니다."));
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "대상 사용자를 찾을 수 없습니다."));

        List<ChatRoomMember> existingMembers = chatRoomMemberRepository.findActiveRoomsBetween(currentUserId, targetUserId);
        if (!existingMembers.isEmpty()) {
            ChatRoomMember member = existingMembers.get(0);
            ChatRoom room = member.getChatRoom();
            return ChatRoomResponse.builder()
                    .roomId(room.getId())
                    .roomUuid(room.getRoomUuid())
                    .otherUserId(targetUser.getId())
                    .otherUserNickname(targetUser.getNickname())
                    .otherUserAvatarUrl(targetUser.getDessertAvatarUrl())
                    .lastMessagePreview(room.getLastMessagePreview())
                    .lastMessageAt(room.getLastMessageAt())
                    .unreadCount(chatMessageQueryRepository.countUnread(room.getId(), currentUserId))
                    .build();
        }

        // 새 방은 운영자(ADMIN)가 끼어 있거나, 두 사람 사이에 수락된 대화 요청이 있을 때만 연다 (하트·수락 절차 우회 방지)
        boolean operatorChat = currentUser.getRole() == UserRole.ROLE_ADMIN || targetUser.getRole() == UserRole.ROLE_ADMIN;
        boolean accepted = chatRequestRepository.existsBySenderIdAndReceiverIdAndStatus(currentUserId, targetUserId, RequestStatus.ACCEPTED)
                || chatRequestRepository.existsBySenderIdAndReceiverIdAndStatus(targetUserId, currentUserId, RequestStatus.ACCEPTED);
        if (!operatorChat && !accepted) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "대화 요청이 수락된 상대와만 대화방을 열 수 있습니다.");
        }

        ChatRoom chatRoom = ChatRoom.create();
        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);

        chatRoomMemberRepository.save(ChatRoomMember.of(savedRoom, currentUser));
        chatRoomMemberRepository.save(ChatRoomMember.of(savedRoom, targetUser));

        log.info("1:1 직통 채팅방 개설 완료: roomId={}, userA={}, userB={}",
                savedRoom.getId(), currentUserId, targetUserId);

        return ChatRoomResponse.builder()
                .roomId(savedRoom.getId())
                .roomUuid(savedRoom.getRoomUuid())
                .otherUserId(targetUser.getId())
                .otherUserNickname(targetUser.getNickname())
                .otherUserAvatarUrl(targetUser.getDessertAvatarUrl())
                .lastMessagePreview(savedRoom.getLastMessagePreview())
                .lastMessageAt(savedRoom.getLastMessageAt())
                .unreadCount(0L)
                .build();
    }

    /** 상대 화면에 "OO님이 채팅방을 나갔습니다" SYSTEM 메시지를 남기고 실시간으로 보낸다 */
    private void publishLeaveNotice(ChatRoomMember leaver) {
        User user = leaver.getUser();
        Long roomId = leaver.getChatRoom().getId();
        String content = user.getNickname() + "님이 채팅방을 나갔습니다.";
        ChatMessageDocument saved = chatMessageRepository.save(ChatMessageDocument.builder()
                .roomId(roomId)
                .senderId(user.getId())
                .senderNickname(user.getNickname())
                .messageType(SYSTEM_TYPE)
                .content(content)
                .readUserIds(new ArrayList<>(Collections.singletonList(user.getId())))
                .createdAt(LocalDateTime.now())
                .build());
        leaver.getChatRoom().updateLastMessage(content);
        redisPublisher.publish(ChannelTopic.of("chat:room:" + roomId), ChatMessageDto.from(saved, user.getDessertAvatarUrl()));
    }
}
