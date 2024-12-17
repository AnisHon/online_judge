package com.anishan.judge.util;

import com.anishan.judge.config.JudgeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class JudgeDelayUtil {

    private final StringRedisTemplate stringRedisTemplate;
    private final JudgeConfig judgeConfig;

    private static String getKey(Long userId) {
        return "judge:delay:" + userId;
    }

    public boolean isAvailable(Long userId) {
        return !stringRedisTemplate.hasKey(getKey(userId));
    }

    public void setDelay(Long userId) {
        stringRedisTemplate.opsForValue().set(getKey(userId), "", judgeConfig.getJudgeInterval(), TimeUnit.SECONDS);
    }



}
