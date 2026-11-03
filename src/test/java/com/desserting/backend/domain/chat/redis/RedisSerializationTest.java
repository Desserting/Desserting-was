package com.desserting.backend.domain.chat.redis;

import com.desserting.backend.domain.chat.dto.ChatMessageDto;
import com.desserting.backend.global.config.RedisConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisSerializationTest {

    @Test
    @DisplayName("ChatMessageDto에 LocalDateTime이 포함되어 있어도 GenericJackson2JsonRedisSerializer 직렬화 및 역직렬화 성공")
    @SuppressWarnings("unchecked")
    void testLocalDateTimeSerialization() {
        // given
        RedisConfig config = new RedisConfig();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisTemplate<String, Object> template = config.redisTemplate(connectionFactory);
        RedisSerializer<Object> serializer = (RedisSerializer<Object>) template.getValueSerializer();

        LocalDateTime now = LocalDateTime.of(2026, 11, 3, 20, 50, 0);
        ChatMessageDto originalDto = ChatMessageDto.builder()
                .messageId("msg-12345")
                .roomId(10L)
                .senderId(1L)
                .content("디저트 데이트 하실래요?")
                .messageType("TEXT")
                .createdAt(now)
                .build();

        // when
        byte[] serialized = serializer.serialize(originalDto);
        assertThat(serialized).isNotEmpty();

        Object deserialized = serializer.deserialize(serialized);

        // then
        assertThat(deserialized).isInstanceOf(ChatMessageDto.class);
        ChatMessageDto resultDto = (ChatMessageDto) deserialized;
        assertThat(resultDto.getMessageId()).isEqualTo("msg-12345");
        assertThat(resultDto.getContent()).isEqualTo("디저트 데이트 하실래요?");
        assertThat(resultDto.getCreatedAt()).isEqualTo(now);
    }
}
