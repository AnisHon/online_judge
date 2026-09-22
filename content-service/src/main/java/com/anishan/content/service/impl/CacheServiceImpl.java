package com.anishan.content.service.impl;

import com.anishan.content.domain.vo.CacheKeyPage;
import com.anishan.content.domain.vo.CacheVo;
import com.anishan.content.service.CacheCatalog;
import com.anishan.content.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CacheServiceImpl implements CacheService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public CacheKeyPage list(String prefix, String cursor, int limit) {
        requireManagedPrefix(prefix);
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        long cursorValue = parseCursor(cursor);
        Object raw = stringRedisTemplate.execute((RedisCallback<Object>) connection -> connection.execute(
                "SCAN",
                bytes(String.valueOf(cursorValue)),
                bytes("MATCH"),
                bytes(prefix + "*"),
                bytes("COUNT"),
                bytes(String.valueOf(safeLimit))
        ));
        if (!(raw instanceof List)) return new CacheKeyPage();

        List<?> response = (List<?>) raw;
        String nextCursor = response.size() > 0 ? text(response.get(0)) : "0";
        List<String> keys = new ArrayList<>();
        if (response.size() > 1 && response.get(1) instanceof List) {
            for (Object key : (List<?>) response.get(1)) {
                String value = text(key);
                if (value != null) keys.add(value);
            }
        }
        Collections.sort(keys);
        return new CacheKeyPage()
                .setKeys(keys)
                .setNextCursor(nextCursor)
                .setHasMore(!"0".equals(nextCursor));
    }

    @Override
    public CacheVo get(String key) {
        requireManagedKey(key);

        DataType dataType = stringRedisTemplate.type(key);
        Long expire = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        String value = readValue(key, dataType);
        if (isSensitiveKey(key)) {
            value = "[敏感缓存已隐藏]";
        } else if (value != null && value.length() > 20_000) {
            value = value.substring(0, 20_000) + "\n… [内容已截断]";
        }

        return new CacheVo()
                .setExpireTime(expire)
                .setKey(key)
                .setValue(value);
    }

    @Override
    public boolean remove(String key) {
        requireManagedKey(key);
        return Boolean.TRUE.equals(stringRedisTemplate.delete(key));
    }

    private String readValue(String key, DataType dataType) {
        if (dataType == null || DataType.NONE.equals(dataType)) {
            return null;
        }
        switch (dataType) {
            case STRING:
                return stringRedisTemplate.opsForValue().get(key);
            case HASH:
                return stringify(stringRedisTemplate.opsForHash().entries(key));
            case LIST:
                return stringify(stringRedisTemplate.opsForList().range(key, 0, 200));
            case SET:
                return stringify(stringRedisTemplate.opsForSet().members(key));
            case ZSET:
                return stringify(stringRedisTemplate.opsForZSet().range(key, 0, 200));
            default:
                return "[暂不支持读取的数据类型: " + dataType.code() + "]";
        }
    }

    private static String stringify(Object value) {
        if (value == null) return null;
        if (value instanceof Map) return new LinkedHashMap<>((Map<?, ?>) value).toString();
        if (value instanceof Set) return String.valueOf(value);
        return String.valueOf(value);
    }

    private static void requireManagedPrefix(String prefix) {
        if (!CacheCatalog.isManagedPrefix(prefix)) {
            throw new IllegalArgumentException("Unsupported cache prefix");
        }
    }

    private static void requireManagedKey(String key) {
        if (!CacheCatalog.isManagedKey(key)) {
            throw new IllegalArgumentException("Unsupported cache key");
        }
    }

    private static long parseCursor(String cursor) {
        try {
            return Math.max(Long.parseLong(cursor), 0);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String text(Object value) {
        if (value instanceof byte[]) return new String((byte[]) value, StandardCharsets.UTF_8);
        return value == null ? null : String.valueOf(value);
    }

    private static boolean isSensitiveKey(String key) {
        String normalized = key == null ? "" : key.toLowerCase();
        return normalized.startsWith("user-service:")
                || normalized.startsWith("problem:submit_log:")
                || normalized.startsWith("judge:")
                || normalized.startsWith("file:splice:")
                || normalized.contains("token") || normalized.contains("captcha")
                || normalized.contains("session") || normalized.contains("password")
                || normalized.contains("secret") || normalized.contains("refresh");
    }
}
