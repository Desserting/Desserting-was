package com.desserting.backend.domain.chat.websocket;

import com.desserting.backend.domain.chat.repository.ChatRoomMemberRepository;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import com.desserting.backend.global.security.CustomUserDetails;
import com.desserting.backend.global.security.CustomUserDetailsService;
import com.desserting.backend.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    /** RedisSubscriber 가 두 토픽 모두로 방송하므로 두 경로 모두 참여자 검증을 한다 */
    private static final List<String> ROOM_TOPIC_PREFIXES = List.of("/sub/chat/room/", "/sub/chats/rooms/");

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        // 1. WebSocket 핸드셰이크 후 STOMP CONNECT 시 JWT 인증 처리 (Access Token 전용)
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = resolveToken(accessor);
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token) && jwtTokenProvider.isAccessToken(token)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                UserDetails userDetails = userDetailsService.loadUserById(userId);
                Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                accessor.setUser(auth);
                log.info("STOMP CONNECT 성공: userId={}", userId);
            } else {
                log.warn("STOMP CONNECT 실패: 유효하지 않은 JWT 토큰 또는 Access Token이 아님");
                throw new CustomException(ErrorCode.UNAUTHORIZED, "웹소켓 연결 인증에 실패하였습니다.");
            }
        }

        // 2. 특정 채팅방 구독(/sub/chat/room/{roomId}) 시 참여 권한 검증
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            String roomPrefix = destination == null ? null
                    : ROOM_TOPIC_PREFIXES.stream().filter(destination::startsWith).findFirst().orElse(null);
            if (roomPrefix != null) {
                Authentication auth = (Authentication) accessor.getUser();
                if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails userDetails)) {
                    log.warn("STOMP SUBSCRIBE 거부: 미인증 세션의 구독 시도 destination={}", destination);
                    throw new CustomException(ErrorCode.UNAUTHORIZED, "인증된 사용자만 채팅방을 구독할 수 있습니다.");
                }

                Long roomId;
                try {
                    roomId = Long.parseLong(destination.substring(roomPrefix.length()));
                } catch (NumberFormatException e) {
                    log.warn("STOMP SUBSCRIBE 거부: 유효하지 않은 방 번호 destination={}", destination);
                    throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 채팅방 경로입니다.");
                }

                Long userId = userDetails.getUserId();
                boolean isMember = chatRoomMemberRepository.existsByChatRoomIdAndUserIdAndIsLeftFalse(roomId, userId);
                if (!isMember) {
                    log.warn("STOMP SUBSCRIBE 거부: userId={}는 roomId={}의 참여자가 아닙니다.", userId, roomId);
                    throw new CustomException(ErrorCode.CHAT_MEMBER_NOT_ALLOWED, "채팅방 참여자만 구독할 수 있습니다.");
                }
                log.info("STOMP SUBSCRIBE 인가 성공: userId={}, roomId={}", userId, roomId);
            }
        }

        return message;
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String bearerToken = accessor.getFirstNativeHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
