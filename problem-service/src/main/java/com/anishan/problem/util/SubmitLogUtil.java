package com.anishan.problem.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.anishan.problem.domain.entity.SubmitLog;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SubmitLogUtil {

    private final RedisTemplate<String, Object> redisTemplate;


    private static String getSubmitLogKey(Long userId) {
        return "problem:submit_log:" + userId;
    }

    public void cacheLog(SubmitLog submitLog) {
        String key = getSubmitLogKey(submitLog.getUserId());
        redisTemplate.opsForValue().set(key, submitLog);
    }

    public boolean isExist(Long userId) {
        String key = getSubmitLogKey(userId);
        return redisTemplate.hasKey(key);
    }

    public void update(SubmitLog submitLog) {
        String key = getSubmitLogKey(submitLog.getUserId());

        SubmitLog submitLogOrigin = Objects.requireNonNullElse(get(submitLog.getUserId()), new SubmitLog());

        BeanUtil.copyProperties(
                submitLog,
                submitLogOrigin,
                CopyOptions.create().setIgnoreNullValue(true).setIgnoreError(true));

        redisTemplate.opsForValue().set(key, submitLogOrigin);
    }

    public void setExpired(Long userId, Long seconds) {
        String key = getSubmitLogKey(userId);
        redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
    }

    public SubmitLog get(Long userId) {
        String key = getSubmitLogKey(userId);
        return (SubmitLog) redisTemplate.opsForValue().get(key);
    }

}
