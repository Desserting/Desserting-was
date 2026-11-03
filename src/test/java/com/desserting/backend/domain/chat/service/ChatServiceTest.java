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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisPublisher redisPublisher;

    @InjectMocks
    private ChatService chatService;

    @Test
    @DisplayName("정상 메시지 전송 시 MongoDB 저장, ChatRoom 최신메시지 갱신, Redis 발행이 순차 실행된다")
    void sendMessage_Success() {
        // given
        Long roomId = 10L;
        Long senderId = 1L;

        User sender = User.builder().email("sender@desserting.com").nickname("초코크로와상").build();
        ReflectionTestUtils.setField(sender, "id", senderId);
        ReflectionTestUtils.setField(sender, "dessertAvatarUrl", "https://avatar.png");

        ChatRoom chatRoom = ChatRoom.create();
        ReflectionTestUtils.setField(chatRoom, "id", roomId);

        ChatRoomMember member = ChatRoomMember.builder()
                .chatRoom(chatRoom)
                .user(sender)
                .isLeft(false)
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, senderId)).willReturn(Optional.of(member));

        given(chatMessageRepository.save(any(ChatMessageDocument.class))).willAnswer(invocation -> {
            ChatMessageDocument doc = invocation.getArgument(0);
            ReflectionTestUtils.setField(doc, "id", "mongo-doc-id-123");
            return doc;
        });

        ChatMessageRequest request = new ChatMessageRequest(roomId, "TEXT", "안녕하세요! 동성로 카페 어때요?", null);

        // when
        ChatMessageDto result = chatService.sendMessage(senderId, request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMessageId()).isEqualTo("mongo-doc-id-123");
        assertThat(result.getContent()).isEqualTo("안녕하세요! 동성로 카페 어때요?");
        assertThat(result.getSenderNickname()).isEqualTo("초코크로와상");
        assertThat(chatRoom.getLastMessagePreview()).isEqualTo("안녕하세요! 동성로 카페 어때요?");

        verify(chatMessageRepository).save(any(ChatMessageDocument.class));
        verify(redisPublisher).publish(eq(ChannelTopic.of("chat:room:" + roomId)), any(ChatMessageDto.class));
    }

    @Test
    @DisplayName("채팅방 멤버가 아닌 유저가 메시지 전송 시 CHAT_MEMBER_NOT_ALLOWED 예외 발생")
    void sendMessage_NotMember() {
        // given
        Long roomId = 10L;
        Long senderId = 999L;
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, senderId)).willReturn(Optional.empty());

        ChatMessageRequest request = new ChatMessageRequest(roomId, "TEXT", "테스트", null);

        // when & then
        assertThatThrownBy(() -> chatService.sendMessage(senderId, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_MEMBER_NOT_ALLOWED);
    }

    @Test
    @DisplayName("이미 퇴장한 멤버가 메시지 전송 시 CHAT_MEMBER_NOT_ALLOWED 예외 발생")
    void sendMessage_LeftMember() {
        // given
        Long roomId = 10L;
        Long senderId = 1L;

        User sender = User.builder().email("sender@desserting.com").nickname("퇴장유저").build();
        ReflectionTestUtils.setField(sender, "id", senderId);

        ChatRoom chatRoom = ChatRoom.create();
        ChatRoomMember member = ChatRoomMember.builder()
                .chatRoom(chatRoom)
                .user(sender)
                .isLeft(true)
                .build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, senderId)).willReturn(Optional.of(member));

        ChatMessageRequest request = new ChatMessageRequest(roomId, "TEXT", "퇴장한 상태 메시지", null);

        // when & then
        assertThatThrownBy(() -> chatService.sendMessage(senderId, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_MEMBER_NOT_ALLOWED);
    }

    @Test
    @DisplayName("내 활성 채팅방 목록 조회 시 상대방 프로필과 마지막 메시지 정보가 반환된다")
    void getMyChatRooms_Success() {
        // given
        Long myId = 1L;
        Long otherId = 2L;

        User me = User.builder().nickname("나").build();
        ReflectionTestUtils.setField(me, "id", myId);

        User other = User.builder().nickname("상대방").build();
        ReflectionTestUtils.setField(other, "id", otherId);
        ReflectionTestUtils.setField(other, "dessertAvatarUrl", "https://other-avatar.png");

        ChatRoom room = ChatRoom.create();
        ReflectionTestUtils.setField(room, "id", 100L);
        room.updateLastMessage("반갑습니다");

        ChatRoomMember myMembership = ChatRoomMember.builder().chatRoom(room).user(me).isLeft(false).build();
        ChatRoomMember otherMembership = ChatRoomMember.builder().chatRoom(room).user(other).isLeft(false).build();

        given(chatRoomMemberRepository.findAllMyActiveMemberships(myId)).willReturn(List.of(myMembership));
        given(chatRoomMemberRepository.findOtherMember(100L, myId)).willReturn(Optional.of(otherMembership));

        // when
        List<ChatRoomResponse> rooms = chatService.getMyChatRooms(myId);

        // then
        assertThat(rooms).hasSize(1);
        ChatRoomResponse response = rooms.get(0);
        assertThat(response.getRoomId()).isEqualTo(100L);
        assertThat(response.getOtherUserId()).isEqualTo(otherId);
        assertThat(response.getOtherUserNickname()).isEqualTo("상대방");
        assertThat(response.getLastMessagePreview()).isEqualTo("반갑습니다");
    }

    @Test
    @DisplayName("채팅 메시지 커서 기반 페이징 조회 성공")
    void getChatMessages_Success() {
        // given
        Long roomId = 10L;
        Long userId = 1L;

        User user = User.builder().nickname("유저1").build();
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(user, "dessertAvatarUrl", "https://avatar.png");

        ChatRoom chatRoom = ChatRoom.create();
        ChatRoomMember member = ChatRoomMember.builder().chatRoom(chatRoom).user(user).isLeft(false).build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(member));

        ChatMessageDocument doc1 = ChatMessageDocument.builder()
                .roomId(roomId)
                .senderId(userId)
                .senderNickname("유저1")
                .messageType("TEXT")
                .content("첫 번째 메시지")
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .readUserIds(new ArrayList<>(List.of(userId)))
                .build();
        ReflectionTestUtils.setField(doc1, "id", "doc-1");

        given(chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(eq(roomId), any(PageRequest.class)))
                .willReturn(List.of(doc1));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        List<ChatMessageDto> messages = chatService.getChatMessages(userId, roomId, null, 20);

        // then
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getContent()).isEqualTo("첫 번째 메시지");
        assertThat(messages.get(0).getSenderAvatarUrl()).isEqualTo("https://avatar.png");
    }

    @Test
    @DisplayName("채팅방 나가기 호출 시 멤버 상태가 isLeft=true로 변경된다")
    void leaveChatRoom_Success() {
        // given
        Long roomId = 10L;
        Long userId = 1L;

        ChatRoom chatRoom = ChatRoom.create();
        User user = User.builder().nickname("유저1").build();
        ChatRoomMember member = ChatRoomMember.builder().chatRoom(chatRoom).user(user).isLeft(false).build();
        ReflectionTestUtils.setField(member, "id", 55L);

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, userId)).willReturn(Optional.of(member));

        // when
        chatService.leaveChatRoom(userId, roomId);

        // then
        verify(chatRoomMemberRepository).save(any(ChatRoomMember.class));
    }
}
