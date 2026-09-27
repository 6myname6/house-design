package com.housedesign.common;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedisChatMemoryStore implements ChatMemoryStore {
    private static final String KEY_PREFIX = "ai:memory:";
    private final StringRedisTemplate stringRedisTemplate;
    @Value("${app.ai.memory-ttl-seconds}")
    private long ttlSeconds;

    @Override
    public void deleteMessages(Object memoryId) {
        String key = KEY_PREFIX + memoryId;
        stringRedisTemplate.delete(key.toString());
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        // 从redis取对话消息
        String key = KEY_PREFIX + memoryId;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null) {
            return List.of();
        }
        // josn字符串转化为List<ChatMessage>
        List<ChatMessage> list = ChatMessageDeserializer.messagesFromJson(json);
        return list;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> list) {
        String key = KEY_PREFIX + memoryId;
        // 更新会话消息
        // 1 .先将会话消息序列化
        String json = ChatMessageSerializer.messagesToJson(list);
        // 2.将消息存入redis
        stringRedisTemplate.opsForValue().set(key.toString(), json, Duration.ofSeconds(ttlSeconds));
    }

}
