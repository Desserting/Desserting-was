package com.desserting.backend.domain.chat.redis;

import com.desserting.backend.domain.chat.dto.ChatMessageDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            ChatMessageDto chatMessage = objectMapper.readValue(body, ChatMessageDto.class);

            log.debug("Redis Sub: roomId={}, messageId={}", chatMessage.getRoomId(), chatMessage.getMessageId());
            // STOMP WebSocket 구독자들에게 실시간 브로드캐스팅
            messagingTemplate.convertAndSend("/sub/chat/room/" + chatMessage.getRoomId(), chatMessage);
        } catch (Exception e) {
            log.error("Redis 메시지 역직렬화 및 브로드캐스트 실패: ", e);
        }
    }
}
