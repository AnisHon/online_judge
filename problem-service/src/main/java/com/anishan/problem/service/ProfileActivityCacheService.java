package com.anishan.problem.service;

import com.anishan.api.annotation.EnableCache;
import com.anishan.problem.domain.vo.ProfileActivityBaseVo;
import com.anishan.problem.domain.vo.ProfileActivityDayVo;
import com.anishan.problem.domain.vo.ProfileActivityHeatmapVo;
import com.anishan.problem.domain.vo.ProfileContestVo;
import com.anishan.problem.domain.vo.ProfileProblemItemVo;
import com.anishan.problem.mapper.ProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ProfileActivityCacheService {

    private static final int MAX_SOLVED_PROBLEMS = 300;
    private static final int MAX_CONTESTS = 50;
    private static final ZoneId ACTIVITY_ZONE = ZoneId.of("Asia/Shanghai");

    private final ProfileMapper profileMapper;

    /** Rebuildable, viewer-independent activity only. */
    @EnableCache(name = "problem:profile:v3", expire = 2 * 60 * 60 * 1000)
    public ProfileActivityBaseVo getBase(Long userId) {
        ProfileActivityBaseVo activity = new ProfileActivityBaseVo();
        activity.setUserId(userId);
        activity.setSolvedCount(profileMapper.countSolved(userId));
        activity.setAttemptedCount(profileMapper.countAttempted(userId));
        activity.setHeatmap(buildHeatmap(userId, LocalDate.now(ACTIVITY_ZONE)));

        Map<String, List<String>> grouped = new LinkedHashMap<>();
        grouped.put("easy", new ArrayList<>());
        grouped.put("medium", new ArrayList<>());
        grouped.put("hard", new ArrayList<>());
        grouped.put("unknown", new ArrayList<>());
        List<ProfileProblemItemVo> solvedProblems = profileMapper.selectSolvedProblems(userId, MAX_SOLVED_PROBLEMS + 1);
        activity.setSolvedProblemsTruncated(solvedProblems.size() > MAX_SOLVED_PROBLEMS);
        for (ProfileProblemItemVo item : solvedProblems.stream().limit(MAX_SOLVED_PROBLEMS).collect(Collectors.toList())) {
            int difficulty = item.getDifficulty() == null ? 0 : item.getDifficulty();
            String group;
            if (difficulty == 1) {
                group = "easy";
            } else if (difficulty == 2) {
                group = "medium";
            } else if (difficulty == 3) {
                group = "hard";
            } else {
                group = "unknown";
            }
            grouped.get(group).add(String.valueOf(item.getProblemId()));
        }
        activity.setSolvedProblems(grouped);
        List<ProfileContestVo> contests = profileMapper.selectContests(userId, MAX_CONTESTS + 1);
        activity.setContestsTruncated(contests.size() > MAX_CONTESTS);
        activity.setContests(contests.stream().limit(MAX_CONTESTS).collect(Collectors.toList()));
        return activity;
    }

    /** 包含今天的过去一年；闰年窗口可能有 366 天。缓存跨午夜时保留生成时的边界。 */
    public ProfileActivityHeatmapVo buildHeatmap(Long userId, LocalDate today) {
        LocalDate start = today.minusYears(1).plusDays(1);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ProfileActivityDayVo day : profileMapper.selectActivityDays(
                userId, start.atStartOfDay(), today.plusDays(1).atStartOfDay())) {
            counts.put(day.getDate(), Math.max(0, day.getCount()));
        }
        List<ProfileActivityDayVo> days = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(today); date = date.plusDays(1)) {
            ProfileActivityDayVo day = new ProfileActivityDayVo();
            day.setDate(date.toString());
            day.setCount(counts.getOrDefault(day.getDate(), 0L));
            days.add(day);
        }
        ProfileActivityHeatmapVo heatmap = new ProfileActivityHeatmapVo();
        heatmap.setStartDate(start.toString());
        heatmap.setEndDate(today.toString());
        heatmap.setTimeZone(ACTIVITY_ZONE.getId());
        heatmap.setDays(days);
        return heatmap;
    }
}
