package com.desserting.backend.domain.chat.service;

import com.desserting.backend.domain.chat.document.ChatMessageDocument;
import com.desserting.backend.domain.chat.dto.ChatMessageDto;
import com.desserting.backend.domain.chat.dto.ChatMessageRequest;
import com.desserting.backend.domain.chat.dto.ChatRoomResponse;
import com.desserting.backend.domain.chat.entity.ChatRoom;
import com.desserting.backend.domain.chat.entity.ChatRoomMember;
import com.desserting.backend.domain.chat.redis.RedisPublisher;
import com.desserting.backend.domain.chat.repository.ChatMessageRepository;
import com.desserting.backend.domain.chat.repository.ChatRoomMemberRepository;
import com.desserting.backend.domain.chat.repository.ChatRoomRepository;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.repository.UserRepository;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final RedisPublisher redisPublisher;

    @Transactional
    public ChatMessageDto sendMessage(Long senderId, ChatMessageRequest request) {
        Long roomId = request.getRoomId();

        ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, senderId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_MEMBER_NOT_ALLOWED));

        if (Boolean.TRUE.equals(member.getIsLeft())) {
            throw new CustomException(ErrorCode.CHAT_MEMBER_NOT_ALLOWED, "이미 퇴장한 채팅방입니다.");
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
                    .unreadCount(0L)
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

        List<ChatMessageDocument> documents;
        PageRequest pageRequest = PageRequest.of(0, Math.min(size, 100));

        if (cursorId != null && !cursorId.isBlank()) {
            documents = chatMessageRepository.findByRoomIdAndIdLessThanOrderByCreatedAtDesc(roomId, cursorId, pageRequest);
        } else {
            documents = chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageRequest);
        }

        // DTO 변환
        return documents.stream()
                .map(doc -> {
                    User sender = userRepository.findById(doc.getSenderId()).orElse(null);
                    String avatarUrl = sender != null ? sender.getDessertAvatarUrl() : null;
                    return ChatMessageDto.from(doc, avatarUrl);
                })
                .toList();
    }

    @Transactional
    public void leaveChatRoom(Long userId, Long roomId) {
        ChatRoomMember member = chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_MEMBER_NOT_ALLOWED));

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
        log.info("채팅방 퇴장 완료: userId={}, roomId={}", userId, roomId);
    }
}
