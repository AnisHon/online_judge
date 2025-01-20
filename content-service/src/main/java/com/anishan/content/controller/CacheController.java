package com.anishan.content.controller;

import cn.hutool.core.collection.ListUtil;
import com.anishan.commons.domain.R;
import com.anishan.content.domain.vo.CacheTypeVo;
import com.anishan.content.domain.vo.CacheVo;
import com.anishan.content.service.CacheService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/cache")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CacheController {


    private final CacheService cacheService;
    private final StringRedisTemplate stringRedisTemplate;



    private static final List<CacheTypeVo> cacheTypes;

    static {
        cacheTypes = ListUtil
                .toList(
                        new CacheTypeVo(1, "problem:tag:", "题目标签缓存"),
                        new CacheTypeVo(2, "problem:contest:", "比赛标签缓存"),
                        new CacheTypeVo(3, "user:role:", "用户角色Id缓存"),
                        new CacheTypeVo(4, "problem:folder", "文件夹缓存"),
                        new CacheTypeVo(5, "problem:detail:", "题目缓存"),
                        new CacheTypeVo(6, "problem:recent:", "最近题目缓存"),
                        new CacheTypeVo(7, "contest:problem:", "比赛题目列表缓存"),
                        new CacheTypeVo(8, "user:rank:", "比赛题目列表缓存"),
                        new CacheTypeVo(8, "problem:choice-fill:", "填空选择答案缓存"),
                        new CacheTypeVo(9, "content:file:", "文件缓存")
                        );
    }

    @ApiOperation("列出所有的缓存类型")
    @GetMapping("/type")
    public R<List<CacheTypeVo>> type() {
        return R.success(cacheTypes);
    }

    @ApiOperation("列出指定缓存键")
    @GetMapping("/list/{prefix}")
    @PreAuthorize("hasAuthority('content:cache:list')")
    public R<List<String>> list(@PathVariable String prefix) {
        List<String> caches =  cacheService.list(prefix);
        return R.success(caches);
    }

    @ApiOperation("获取某个缓存")
    @GetMapping("/{key}")
    @PreAuthorize("hasAuthority('content:cache:list')")
    public R<CacheVo> get(@PathVariable String key) {
        CacheVo cacheVo = cacheService.get(key);
        return R.success(cacheVo);
    }

    @ApiOperation("删除某个缓存")
    @DeleteMapping("/{key}")
    @PreAuthorize("hasAuthority('content:cache:remove')")
    public R<CacheVo> del(@PathVariable String key) {
        stringRedisTemplate.delete(key);
        return R.success(null);
    }

}
