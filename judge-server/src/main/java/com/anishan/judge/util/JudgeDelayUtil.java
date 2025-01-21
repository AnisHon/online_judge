package com.anishan.judge.util;

import com.anishan.judge.config.JudgeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class JudgeDelayUtil {

    private static StringRedisTemplate stringRedisTemplate;
    private static JudgeConfig judgeConfig;


    @Autowired
    public JudgeDelayUtil(StringRedisTemplate stringRedisTemplate, JudgeConfig judgeConfig) {
        JudgeDelayUtil.judgeConfig = judgeConfig;
        JudgeDelayUtil.stringRedisTemplate = stringRedisTemplate;
    }

    private static String getKey(Long userId) {
        return "judge:delay:" + userId;
    }

    public static boolean isAvailable(Long userId) {
        return !stringRedisTemplate.hasKey(getKey(userId));
    }

    public static void setDelay(Long userId) {
        stringRedisTemplate.opsForValue().set(getKey(userId), "", judgeConfig.getJudgeInterval(), TimeUnit.SECONDS);
    }



}
