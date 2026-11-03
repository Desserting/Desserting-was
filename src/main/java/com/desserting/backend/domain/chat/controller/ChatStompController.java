package com.desserting.backend.domain.chat.controller;

import com.desserting.backend.domain.chat.dto.ChatMessageRequest;
import com.desserting.backend.domain.chat.service.ChatService;
import com.desserting.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatService chatService;

    /**
     * 클라이언트가 /pub/chat/message 로 발행한 STOMP 메시지 수신 및 처리
     */
    @MessageMapping("/chat/message")
    public void handleMessage(@Payload ChatMessageRequest request, Principal principal) {
        Long senderId = null;
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            senderId = userDetails.getUserId();
        }

        if (senderId == null) {
            log.error("STOMP 메시지 수신 실패: 인증 정보가 유효하지 않습니다.");
            return;
        }

        log.info("STOMP 메시지 수신: senderId={}, roomId={}, type={}", senderId, request.getRoomId(), request.getMessageType());
        chatService.sendMessage(senderId, request);
    }
}
