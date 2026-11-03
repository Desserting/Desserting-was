package com.desserting.backend.domain.chat.websocket;

import com.desserting.backend.domain.chat.repository.ChatRoomMemberRepository;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.entity.UserRole;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import com.desserting.backend.global.security.CustomUserDetails;
import com.desserting.backend.global.security.CustomUserDetailsService;
import com.desserting.backend.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class StompHandlerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @InjectMocks
    private StompHandler stompHandler;

    @Test
    @DisplayName("STOMP CONNECT - 유효한 Access Token 제공 시 인증 성공 및 User 세션 할당")
    void preSend_Connect_ValidAccessToken_Success() {
        // given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer valid_access_token");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        User mockUser = User.builder().email("user@desserting.com").role(UserRole.ROLE_USER).build();
        ReflectionTestUtils.setField(mockUser, "id", 100L);
        CustomUserDetails userDetails = CustomUserDetails.create(mockUser);

        given(jwtTokenProvider.validateToken("valid_access_token")).willReturn(true);
        given(jwtTokenProvider.isAccessToken("valid_access_token")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("valid_access_token")).willReturn(100L);
        given(userDetailsService.loadUserById(100L)).willReturn(userDetails);

        // when
        Message<?> result = stompHandler.preSend(message, channel);

        // then
        assertThat(result).isNotNull();
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertThat(resultAccessor.getUser()).isNotNull();
        assertThat(((CustomUserDetails) ((Authentication) resultAccessor.getUser()).getPrincipal()).getUserId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("STOMP CONNECT - Refresh Token 사용 시 인증 실패 및 예외 발생")
    void preSend_Connect_RefreshToken_ThrowsUnauthorized() {
        // given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer refresh_token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        given(jwtTokenProvider.validateToken("refresh_token")).willReturn(true);
        given(jwtTokenProvider.isAccessToken("refresh_token")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> stompHandler.preSend(message, channel))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("STOMP SUBSCRIBE - 인증되지 않은 사용자가 채팅방 구독 시 401 차단")
    void preSend_Subscribe_Unauthenticated_ThrowsUnauthorized() {
        // given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/sub/chat/room/1");
        // user session is null
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        // when & then
        assertThatThrownBy(() -> stompHandler.preSend(message, channel))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("STOMP SUBSCRIBE - 참여자가 아닌 채팅방 구독 시 CHAT_MEMBER_NOT_ALLOWED 차단")
    void preSend_Subscribe_NonMember_ThrowsForbidden() {
        // given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/sub/chat/room/99");

        User mockUser = User.builder().email("user@desserting.com").role(UserRole.ROLE_USER).build();
        ReflectionTestUtils.setField(mockUser, "id", 100L);
        CustomUserDetails userDetails = CustomUserDetails.create(mockUser);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        accessor.setUser(auth);

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        given(chatRoomMemberRepository.existsByChatRoomIdAndUserIdAndIsLeftFalse(99L, 100L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> stompHandler.preSend(message, channel))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_MEMBER_NOT_ALLOWED);
    }

    @Test
    @DisplayName("STOMP SUBSCRIBE - 정회원 채팅방 구독 시 인가 성공")
    void preSend_Subscribe_Member_Success() {
        // given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/sub/chat/room/1");

        User mockUser = User.builder().email("user@desserting.com").role(UserRole.ROLE_USER).build();
        ReflectionTestUtils.setField(mockUser, "id", 100L);
        CustomUserDetails userDetails = CustomUserDetails.create(mockUser);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        accessor.setUser(auth);

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        given(chatRoomMemberRepository.existsByChatRoomIdAndUserIdAndIsLeftFalse(1L, 100L)).willReturn(true);

        // when
        Message<?> result = stompHandler.preSend(message, channel);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("STOMP SUBSCRIBE - 잘못된 형식의 roomId 경로 구독 시 INVALID_INPUT_VALUE 예외 발생")
    void preSend_Subscribe_InvalidRoomId_ThrowsInvalidInput() {
        // given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/sub/chat/room/abc");

        User mockUser = User.builder().email("user@desserting.com").role(UserRole.ROLE_USER).build();
        ReflectionTestUtils.setField(mockUser, "id", 100L);
        CustomUserDetails userDetails = CustomUserDetails.create(mockUser);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        accessor.setUser(auth);

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        // when & then
        assertThatThrownBy(() -> stompHandler.preSend(message, channel))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
    }
}
