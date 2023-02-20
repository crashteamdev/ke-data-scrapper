package dev.crashteam.ke_data_scrapper.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisMessagePublisher implements MessagePublisher {

    @Override
    public void publish(String topic, Object message) {
    }
}
