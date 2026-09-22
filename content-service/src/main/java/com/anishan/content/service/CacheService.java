package com.anishan.content.service;

import com.anishan.content.domain.vo.CacheKeyPage;
import com.anishan.content.domain.vo.CacheVo;

public interface CacheService {
    CacheKeyPage list(String prefix, String cursor, int limit);

    CacheVo get(String key);

    boolean remove(String key);
}
