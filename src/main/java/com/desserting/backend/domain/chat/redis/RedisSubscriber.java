package com.desserting.backend.domain.chat.redis;

import com.desserting.backend.domain.chat.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // RedisPublisher 가 RedisTemplate 의 값 직렬화기(타입 정보 포함 JSON)로 발행하므로 같은 직렬화기로 읽는다
            Object payload = redisTemplate.getValueSerializer().deserialize(message.getBody());
            if (!(payload instanceof ChatMessageDto chatMessage)) {
                log.warn("Redis Sub: 처리할 수 없는 메시지 타입 {}", payload == null ? "null" : payload.getClass().getName());
                return;
            }

            log.debug("Redis Sub: roomId={}, messageId={}", chatMessage.getRoomId(), chatMessage.getMessageId());
            // STOMP WebSocket 구독자들에게 실시간 브로드캐스팅
            messagingTemplate.convertAndSend("/sub/chat/room/" + chatMessage.getRoomId(), chatMessage);
        } catch (Exception e) {
            log.error("Redis 메시지 역직렬화 및 브로드캐스트 실패: ", e);
        }
    }
}
