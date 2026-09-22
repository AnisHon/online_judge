package com.anishan.content.controller;

import com.anishan.commons.domain.R;
import com.anishan.content.domain.vo.CacheKeyPage;
import com.anishan.content.domain.vo.CacheVo;
import com.anishan.content.service.CacheService;
import com.anishan.content.service.CacheCatalog;
import com.anishan.content.domain.vo.CacheTypeVo;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cache")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CacheController {


    private final CacheService cacheService;

    @ApiOperation("列出所有的缓存类型")
    @GetMapping("/type")
    @PreAuthorize("hasAuthority('content:cache:list')")
    public R<List<CacheTypeVo>> type() {
        return R.success(CacheCatalog.types());
    }

    @ApiOperation("列出指定缓存键")
    @GetMapping("/list/{prefix}")
    @PreAuthorize("hasAuthority('content:cache:list')")
    public R<CacheKeyPage> list(@PathVariable String prefix,
                                @RequestParam(defaultValue = "0") String cursor,
                                @RequestParam(defaultValue = "100") int limit) {
        return R.success(cacheService.list(prefix, cursor, limit));
    }

    @ApiOperation("获取某个缓存")
    @GetMapping("/{key}")
    @PreAuthorize("hasAuthority('content:cache:read')")
    public R<CacheVo> get(@PathVariable String key) {
        CacheVo cacheVo = cacheService.get(key);
        return R.success(cacheVo);
    }

    @ApiOperation("删除某个缓存")
    @DeleteMapping("/{key}")
    @PreAuthorize("hasAuthority('content:cache:remove')")
    public R<Boolean> del(@PathVariable String key) {
        return R.success(cacheService.remove(key));
    }

}
