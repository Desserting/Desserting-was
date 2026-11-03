package com.desserting.backend.domain.chat.redis;

import com.desserting.backend.domain.chat.dto.ChatMessageDto;
import com.desserting.backend.global.config.RedisConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RedisSubscriberTest {

    private final RedisTemplate<String, Object> redisTemplate =
            new RedisConfig().redisTemplate(mock(RedisConnectionFactory.class));
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final RedisSubscriber subscriber = new RedisSubscriber(redisTemplate, messagingTemplate);

    @Test
    @DisplayName("RedisPublisher 와 같은 직렬화기로 발행된 메시지를 받아 채팅방 구독자에게 브로드캐스트한다")
    void receivesPublishedMessageAndBroadcasts() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 3, 22, 0, 0);
        ChatMessageDto sent = ChatMessageDto.builder()
                .messageId("msg-1").roomId(10L).senderId(1L).senderNickname("딸기쇼트케이크")
                .messageType("TEXT").content("앞산 카페거리 가볼래요?").createdAt(now)
                .build();
        // RedisPublisher.publish() 가 Redis 로 보내는 바이트와 동일
        @SuppressWarnings("unchecked")
        byte[] body = ((org.springframework.data.redis.serializer.RedisSerializer<Object>) redisTemplate.getValueSerializer())
                .serialize(sent);

        subscriber.onMessage(new DefaultMessage("chat:room:10".getBytes(StandardCharsets.UTF_8), body), null);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/sub/chat/room/10"), captor.capture());
        ChatMessageDto received = (ChatMessageDto) captor.getValue();
        assertThat(received.getMessageId()).isEqualTo("msg-1");
        assertThat(received.getContent()).isEqualTo("앞산 카페거리 가볼래요?");
        assertThat(received.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("채팅 메시지가 아닌 값이 들어오면 브로드캐스트하지 않는다")
    void ignoresNonChatPayload() {
        @SuppressWarnings("unchecked")
        byte[] body = ((org.springframework.data.redis.serializer.RedisSerializer<Object>) redisTemplate.getValueSerializer())
                .serialize("not a chat message");

        subscriber.onMessage(new DefaultMessage("chat:room:10".getBytes(StandardCharsets.UTF_8), body), null);

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
}
