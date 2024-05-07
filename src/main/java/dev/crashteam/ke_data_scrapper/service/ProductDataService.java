package dev.crashteam.ke_data_scrapper.service;

import dev.crashteam.ke_data_scrapper.model.ProductCacheData;
import dev.crashteam.ke_data_scrapper.model.RedisKey;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ProductDataService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final HashOperations<String, String, ProductCacheData> hashOperations;

    public ProductDataService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.hashOperations = redisTemplate.opsForHash();
    }

    public boolean save(Long productId) {
        return hashOperations.putIfAbsent(RedisKey.KE_PRODUCT.getKey(), String.valueOf(productId), new ProductCacheData(productId, LocalDateTime.now()));
    }

    public void delete() {
        redisTemplate.delete(RedisKey.KE_PRODUCT.getKey());
    }

}
