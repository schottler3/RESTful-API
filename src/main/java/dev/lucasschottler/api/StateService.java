package dev.lucasschottler.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class StateService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    public void setState(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }
    
    public String getState(String key) {
        return redisTemplate.opsForValue().get(key);
    }
}