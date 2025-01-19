package com.anishan.problem.util;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CacheUtil {

    private final CacheManager cacheManager;

    public void clearMultipleCaches(List<String> cacheNames) {
        for (String cacheName : cacheNames) {
            Objects.requireNonNull(cacheManager.getCache(cacheName)).clear();
        }
    }

    public void clearAllCaches(String cacheName, List<Long> ids) {
        List<String> keys = ids.stream().map(x -> "problem:detail:" + x).collect(Collectors.toList());
        clearMultipleCaches(keys);
    }

}
