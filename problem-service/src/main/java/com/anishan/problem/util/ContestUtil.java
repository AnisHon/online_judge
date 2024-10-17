package com.anishan.problem.util;

import com.anishan.problem.domain.entity.Contest;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.RedisTemplate;

public class ContestUtil {


    public static String getContestKey(Long contestId) {
        return "problem:contest:" + contestId;
    }

    public static void beginContest(@NotNull RedisTemplate<String, Object> redisTemplate, Contest contest) {
        String contestKey = getContestKey(contest.getContestId());
        redisTemplate.opsForValue().set(contestKey, contest);
    }

    public static void endContest(@NotNull RedisTemplate<String, Object> redisTemplate, Long contestId) {
        String contestKey = getContestKey(contestId);
        redisTemplate.delete(contestKey);
    }

    public static boolean isInContest(@NotNull RedisTemplate<String, Object> redisTemplate, Long contestId) {
        String contestKey = getContestKey(contestId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(contestKey));
    }



}
