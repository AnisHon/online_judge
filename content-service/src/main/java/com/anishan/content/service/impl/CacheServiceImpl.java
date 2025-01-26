package com.anishan.content.service.impl;

import com.anishan.content.domain.vo.CacheVo;
import com.anishan.content.service.CacheService;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CacheServiceImpl implements CacheService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public List<String> list(String prefix) {

        @Cleanup
        Cursor<String> scan = stringRedisTemplate.scan(ScanOptions.scanOptions().match(prefix + "*").build());

        return scan.stream().collect(Collectors.toList());
    }

    @Override
    public CacheVo get(String key) {

        String value = stringRedisTemplate.opsForValue().get(key);
        Long expire = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);

        return new CacheVo()
                .setExpireTime(expire)
                .setKey(key)
                .setValue(value);
    }
}
