package com.anishan.api.util;

import com.anishan.api.client.gojudge.domain.TestResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisJudgeTestUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    private String getTestKey(Long userId) {
        return "judge:test:" + userId;
    }

    public void save(TestResult testResult, Integer timeout) {
        String testKey = getTestKey(testResult.getUserId());
        redisTemplate.opsForValue().set(testKey, testResult, timeout, TimeUnit.SECONDS);
    }

    public boolean exists(Long userId) {
        String testKey = getTestKey(userId);
        return redisTemplate.hasKey(testKey);
    }

    public TestResult get(Long userId) {
        String testKey = getTestKey(userId);
        Object o = redisTemplate.opsForValue().get(testKey);
        return (TestResult) o;
    }


}
