package com.anishan.content.service;

import com.anishan.content.domain.vo.CacheVo;

import java.util.List;

public interface CacheService {
    List<String> list(String prefix);

    CacheVo get(String key);
}
