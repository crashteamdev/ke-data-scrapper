package dev.crashteam.ke_data_scrapper.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisMessagePublisher implements MessagePublisher {

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Override
    public void publish(String topic, Object message) {
        redisTemplate.convertAndSend(topic, message);
    }
}
