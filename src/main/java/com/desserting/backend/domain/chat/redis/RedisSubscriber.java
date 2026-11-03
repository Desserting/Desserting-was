package com.desserting.backend.domain.chat.redis;

import com.desserting.backend.domain.chat.dto.ChatMessageDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            ChatMessageDto chatMessage = null;
            try {
                Object deserialized = redisTemplate.getValueSerializer().deserialize(message.getBody());
                if (deserialized instanceof ChatMessageDto dto) {
                    chatMessage = dto;
                }
            } catch (Exception ignored) {
            }

            if (chatMessage == null) {
                String body = new String(message.getBody(), StandardCharsets.UTF_8);
                if (body.startsWith("[")) {
                    JsonNode arrayNode = objectMapper.readTree(body);
                    if (arrayNode.isArray() && arrayNode.size() >= 2) {
                        chatMessage = objectMapper.treeToValue(arrayNode.get(1), ChatMessageDto.class);
                    }
                } else {
                    chatMessage = objectMapper.readValue(body, ChatMessageDto.class);
                }
            }

            if (chatMessage != null) {
                log.debug("Redis Sub: roomId={}, messageId={}", chatMessage.getRoomId(), chatMessage.getMessageId());
                messagingTemplate.convertAndSend("/sub/chat/room/" + chatMessage.getRoomId(), chatMessage);
            }
        } catch (Exception e) {
            log.error("Redis 메시지 역직렬화 및 브로드캐스트 실패: ", e);
        }
    }
}
