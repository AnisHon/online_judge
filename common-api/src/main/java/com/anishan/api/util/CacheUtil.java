package com.anishan.api.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CacheUtil {

    private static CacheManager cacheManager;

    @Autowired
    public CacheUtil(CacheManager cacheManager) {
        CacheUtil.cacheManager = cacheManager;
    }

    public static void evict(String s, Long parentId) {
        Cache cache = cacheManager.getCache(s);
        if (cache != null) {
            cache.evict(parentId);

        }
    }

    public static void clearAllCaches(String cacheName, List<Long> ids) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return;
        }
        for (Long id : ids) {
            cache.evict(id);
        }
    }

}
