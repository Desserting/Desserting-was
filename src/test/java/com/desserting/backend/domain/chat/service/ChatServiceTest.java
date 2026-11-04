package com.desserting.backend.domain.chat.service;

import com.desserting.backend.domain.chat.repository.ChatMessageQueryRepository;
import com.desserting.backend.domain.matching.entity.RequestStatus;
import com.desserting.backend.domain.matching.repository.ChatRequestRepository;
import com.desserting.backend.domain.user.entity.UserRole;
import static org.mockito.Mockito.never;
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

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private ChatMessageQueryRepository chatMessageQueryRepository;

    @Mock
    private ChatRequestRepository chatRequestRepository;

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
    @DisplayName("상대방과 차단 관계인 대화방에서 메시지 전송 시 ACCESS_DENIED 예외 발생")
    void sendMessage_ThrowsException_WhenBlocked() {
        // given
        Long roomId = 10L;
        Long senderId = 1L;
        Long otherId = 2L;

        User sender = User.builder().nickname("발신자").build();
        ReflectionTestUtils.setField(sender, "id", senderId);
        User other = User.builder().nickname("수신자").build();
        ReflectionTestUtils.setField(other, "id", otherId);

        ChatRoom chatRoom = ChatRoom.create();
        ChatRoomMember member = ChatRoomMember.builder().chatRoom(chatRoom).user(sender).isLeft(false).build();
        ChatRoomMember otherMember = ChatRoomMember.builder().chatRoom(chatRoom).user(other).isLeft(false).build();

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, senderId)).willReturn(Optional.of(member));
        given(chatRoomMemberRepository.findOtherMember(roomId, senderId)).willReturn(Optional.of(otherMember));
        given(userBlockRepository.isBlockedBetween(senderId, otherId)).willReturn(true);

        ChatMessageRequest request = new ChatMessageRequest(roomId, "TEXT", "차단 후 메시지", null);

        // when & then
        assertThatThrownBy(() -> chatService.sendMessage(senderId, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
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
        given(chatMessageQueryRepository.countUnread(100L, myId)).willReturn(3L);

        // when
        List<ChatRoomResponse> rooms = chatService.getMyChatRooms(myId);

        // then
        assertThat(rooms).hasSize(1);
        ChatRoomResponse response = rooms.get(0);
        assertThat(response.getRoomId()).isEqualTo(100L);
        assertThat(response.getOtherUserId()).isEqualTo(otherId);
        assertThat(response.getOtherUserNickname()).isEqualTo("상대방");
        assertThat(response.getLastMessagePreview()).isEqualTo("반갑습니다");
        assertThat(response.getUnreadCount()).isEqualTo(3L);
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

        given(chatMessageQueryRepository.findPage(roomId, null, 20)).willReturn(List.of(doc1));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        List<ChatMessageDto> messages = chatService.getChatMessages(userId, roomId, null, 20);

        // then
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getContent()).isEqualTo("첫 번째 메시지");
        assertThat(messages.get(0).getSenderAvatarUrl()).isEqualTo("https://avatar.png");
        verify(chatMessageQueryRepository).markAllRead(roomId, userId);
    }

    @Test
    @DisplayName("이전 페이지(cursorId) 조회는 읽음 처리를 하지 않는다")
    void getChatMessages_cursorDoesNotMarkRead() {
        User user = User.builder().nickname("유저1").build();
        ReflectionTestUtils.setField(user, "id", 1L);
        ChatRoomMember member = ChatRoomMember.builder().chatRoom(ChatRoom.create()).user(user).isLeft(false).build();
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(10L, 1L)).willReturn(Optional.of(member));
        given(chatMessageQueryRepository.findPage(10L, "abc", 30)).willReturn(List.of());

        chatService.getChatMessages(1L, 10L, "abc", 30);

        verify(chatMessageQueryRepository, never()).markAllRead(any(), any());
    }

    @Test
    @DisplayName("수락된 대화 요청도 운영자도 없으면 직통 채팅방을 열 수 없다")
    void directRoom_deniedWithoutAcceptedRequest() {
        User a = User.builder().nickname("A").build();
        ReflectionTestUtils.setField(a, "id", 1L);
        User b = User.builder().nickname("B").build();
        ReflectionTestUtils.setField(b, "id", 2L);
        given(userRepository.findById(1L)).willReturn(Optional.of(a));
        given(userRepository.findById(2L)).willReturn(Optional.of(b));
        given(chatRoomMemberRepository.findActiveRoomsBetween(1L, 2L)).willReturn(List.of());

        assertThatThrownBy(() -> chatService.getOrCreateDirectRoom(1L, 2L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
        verify(chatRoomRepository, never()).save(any());
    }

    @Test
    @DisplayName("대화 요청이 수락된 상대면 (나간 뒤에도) 직통 채팅방을 다시 열 수 있다")
    void directRoom_allowedAfterAcceptedRequest() {
        User a = User.builder().nickname("A").build();
        ReflectionTestUtils.setField(a, "id", 1L);
        User b = User.builder().nickname("B").build();
        ReflectionTestUtils.setField(b, "id", 2L);
        given(userRepository.findById(1L)).willReturn(Optional.of(a));
        given(userRepository.findById(2L)).willReturn(Optional.of(b));
        given(chatRoomMemberRepository.findActiveRoomsBetween(1L, 2L)).willReturn(List.of());
        given(chatRequestRepository.existsBySenderIdAndReceiverIdAndStatus(1L, 2L, RequestStatus.ACCEPTED)).willReturn(false);
        given(chatRequestRepository.existsBySenderIdAndReceiverIdAndStatus(2L, 1L, RequestStatus.ACCEPTED)).willReturn(true);
        given(chatRoomRepository.save(any(ChatRoom.class))).willAnswer(inv -> inv.getArgument(0));

        ChatRoomResponse response = chatService.getOrCreateDirectRoom(1L, 2L);

        assertThat(response.getOtherUserNickname()).isEqualTo("B");
    }

    @Test
    @DisplayName("운영자(ADMIN)와는 요청 없이 직통 채팅방을 열 수 있다")
    void directRoom_allowedWithOperator() {
        User a = User.builder().nickname("A").build();
        ReflectionTestUtils.setField(a, "id", 1L);
        User admin = User.builder().nickname("운영자").role(UserRole.ROLE_ADMIN).build();
        ReflectionTestUtils.setField(admin, "id", 9L);
        given(userRepository.findById(1L)).willReturn(Optional.of(a));
        given(userRepository.findById(9L)).willReturn(Optional.of(admin));
        given(chatRoomMemberRepository.findActiveRoomsBetween(1L, 9L)).willReturn(List.of());
        given(chatRoomRepository.save(any(ChatRoom.class))).willAnswer(inv -> inv.getArgument(0));

        assertThat(chatService.getOrCreateDirectRoom(1L, 9L).getOtherUserNickname()).isEqualTo("운영자");
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
        given(chatMessageRepository.save(any(ChatMessageDocument.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        chatService.leaveChatRoom(userId, roomId);

        // then
        verify(chatRoomMemberRepository).save(any(ChatRoomMember.class));
        // 상대에게 퇴장 안내 SYSTEM 메시지를 저장·발행한다
        verify(chatMessageRepository).save(org.mockito.ArgumentMatchers.argThat(
                (ChatMessageDocument d) -> "SYSTEM".equals(d.getMessageType()) && d.getContent().contains("유저1님이 채팅방을 나갔습니다")));
        verify(redisPublisher).publish(any(), any());
        assertThat(chatRoom.getLastMessagePreview()).isEqualTo("유저1님이 채팅방을 나갔습니다.");
    }

    @Test
    @DisplayName("상대가 나간 방에는 메시지를 보낼 수 없고, 클라이언트는 SYSTEM 메시지를 보낼 수 없다")
    void sendMessage_BlockedWhenOtherLeftOrSystem() {
        User me = User.builder().nickname("나").build();
        ReflectionTestUtils.setField(me, "id", 1L);
        User other = User.builder().nickname("상대").build();
        ReflectionTestUtils.setField(other, "id", 2L);
        ChatRoom room = ChatRoom.create();
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(10L, 1L))
                .willReturn(Optional.of(ChatRoomMember.builder().chatRoom(room).user(me).isLeft(false).build()));

        ChatMessageRequest system = ChatMessageRequest.builder().roomId(10L).messageType("SYSTEM").content("가짜 안내").build();
        assertThatThrownBy(() -> chatService.sendMessage(1L, system))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);

        given(chatRoomMemberRepository.findOtherMember(10L, 1L))
                .willReturn(Optional.of(ChatRoomMember.builder().chatRoom(room).user(other).isLeft(true).build()));
        ChatMessageRequest text = ChatMessageRequest.builder().roomId(10L).messageType("TEXT").content("안녕").build();
        assertThatThrownBy(() -> chatService.sendMessage(1L, text))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_MEMBER_NOT_ALLOWED);
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("차단 관계인 사용자와 1:1 채팅방 개설 시 ACCESS_DENIED 예외 발생")
    void getOrCreateDirectRoom_BlockedUser() {
        // given
        given(userBlockRepository.isBlockedBetween(1L, 2L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> chatService.getOrCreateDirectRoom(1L, 2L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }
}
