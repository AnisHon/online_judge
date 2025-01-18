package com.anishan.content.controller;

import com.anishan.commons.domain.R;
import com.anishan.content.domain.vo.CacheVo;
import com.anishan.content.service.CacheService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/cache")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CacheController {


    private final CacheService cacheService;
    private final StringRedisTemplate stringRedisTemplate;

    @ApiOperation("列出所有缓存键")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('content:cache:list')")
    public R<List<String>> list() {
        List<String> caches =  cacheService.list();
        return R.success(caches);
    }


    @ApiOperation("获取某个缓存")
    @GetMapping
    @PreAuthorize("hasAuthority('content:cache:list')")
    public R<CacheVo> get(@NotNull String key) {
        CacheVo cacheVo = cacheService.get(key);
        return R.success(cacheVo);
    }

    @ApiOperation("删除某个缓存")
    @DeleteMapping
    @PreAuthorize("hasAuthority('content:cache:remove')")
    public R<CacheVo> del(@NotNull String key) {
        stringRedisTemplate.delete(key);
        return R.success(null);
    }

}
