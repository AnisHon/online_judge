package com.anishan.content.service;

import com.anishan.content.domain.vo.CacheTypeVo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Redis 缓存管理允许浏览的键前缀。
 *
 * <p>缓存管理不能把用户传入的任意字符串直接交给 SCAN/DEL，否则一个后台
 * 权限就可以操作 Redis 中所有业务键。这里集中维护已有键空间，同时把过期
 * 的旧前缀保留为兼容项，不负责创建或迁移 Redis 数据。</p>
 */
public final class CacheCatalog {

    private static final List<CacheTypeVo> TYPES = Collections.unmodifiableList(Arrays.asList(
            new CacheTypeVo(0, "user-service:userId:", "登录用户缓存"),
            new CacheTypeVo(1, "problem:tag:", "题目标签缓存"),
            new CacheTypeVo(2, "problem:contest:", "比赛缓存"),
            new CacheTypeVo(3, "user:role:", "用户角色缓存"),
            new CacheTypeVo(4, "problem:folder:", "目录缓存"),
            new CacheTypeVo(5, "problem:detail:", "题目详情缓存"),
            new CacheTypeVo(6, "problem:recent:", "最近题目缓存"),
            new CacheTypeVo(7, "contest:problem:", "比赛题目列表缓存（兼容）"),
            new CacheTypeVo(8, "user:rank:", "用户排名缓存"),
            new CacheTypeVo(9, "problem:profile:v1:", "个人主页缓存"),
            new CacheTypeVo(10, "problem:choice-fill:", "填空/选择答案缓存"),
            new CacheTypeVo(11, "content:file:", "文件列表缓存"),
            new CacheTypeVo(12, "problem:solution:", "题解缓存"),
            new CacheTypeVo(13, "user:menu:", "用户菜单缓存"),
            new CacheTypeVo(14, "user:treed-menu:", "用户菜单树缓存"),
            new CacheTypeVo(15, "file:splice:", "分片上传缓存"),
            new CacheTypeVo(16, "judge:submission:lock:", "判题提交锁（临时）"),
            new CacheTypeVo(17, "judge:test:", "自由测试结果（临时）"),
            new CacheTypeVo(18, "problem:submit_log:", "判题提交临时记录"),
            new CacheTypeVo(19, "user-service:refresh-session:", "刷新会话（值隐藏）"),
            new CacheTypeVo(20, "user-service:refresh-sessions:user:", "用户刷新会话索引（值隐藏）")
    ));

    private CacheCatalog() {
    }

    /** 返回副本，避免 Web 层意外修改目录。 */
    public static List<CacheTypeVo> types() {
        return TYPES.stream()
                .map(item -> new CacheTypeVo(item.getId(), item.getType(), item.getDesc()))
                .collect(Collectors.toList());
    }

    public static boolean isManagedPrefix(String prefix) {
        return prefix != null && TYPES.stream().anyMatch(item -> item.getType().equals(prefix));
    }

    public static boolean isManagedKey(String key) {
        return key != null && TYPES.stream().anyMatch(item -> key.startsWith(item.getType()));
    }
}
