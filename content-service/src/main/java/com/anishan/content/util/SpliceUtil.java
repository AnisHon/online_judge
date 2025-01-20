package com.anishan.content.util;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class SpliceUtil {


    private static StringRedisTemplate stringRedisTemplate;
    private static RedisTemplate<String, Object> redisTemplate;

    private static String getKey(String md5) {
        return "file:splice:" + md5;
    }


    public SpliceUtil(StringRedisTemplate stringRedisTemplate) {
        SpliceUtil.stringRedisTemplate = stringRedisTemplate;
    }


    public static String getUploadId(String md5) {
        return stringRedisTemplate.opsForValue().get(getKey(md5));
    }


}
