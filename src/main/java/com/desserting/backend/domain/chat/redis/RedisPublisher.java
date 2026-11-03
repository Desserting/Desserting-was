package com.desserting.backend.domain.chat.redis;

import com.desserting.backend.domain.chat.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(ChannelTopic topic, ChatMessageDto message) {
        log.debug("Redis Pub: topic={}, messageId={}", topic.getTopic(), message.getMessageId());
        redisTemplate.convertAndSend(topic.getTopic(), message);
    }
}
