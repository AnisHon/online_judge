package com.anishan.content.service;

import com.anishan.content.domain.vo.CacheVo;

import javax.validation.constraints.NotNull;
import java.util.List;

public interface CacheService {
    List<String> list();

    CacheVo get(String key);
}
