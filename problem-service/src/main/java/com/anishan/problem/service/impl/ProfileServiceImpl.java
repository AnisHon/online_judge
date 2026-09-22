package com.anishan.problem.service.impl;

import com.anishan.api.annotation.EnableCache;
import com.anishan.problem.domain.vo.ProfileActivityVo;
import com.anishan.problem.domain.vo.ProfileContestVo;
import com.anishan.problem.domain.vo.ProfileProblemItemVo;
import com.anishan.problem.domain.vo.ProfileSolutionVo;
import com.anishan.problem.mapper.ProfileMapper;
import com.anishan.problem.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ProfileServiceImpl implements ProfileService {

    private static final int MAX_SOLVED_PROBLEMS = 300;
    private static final int MAX_CONTESTS = 50;
    private static final int MAX_SOLUTIONS = 50;

    private final ProfileMapper profileMapper;

    /**
     * 主页允许适度陈旧，避免首页/个人页频繁扫描提交表。
     * includePrivate 也会进入缓存 key，避免把本人的私有题解误发给访客。
     */
    @Override
    @EnableCache(name = "problem:profile:v1:", expire = 2 * 60 * 60 * 1000)
    public ProfileActivityVo getActivity(Long userId, boolean includePrivate) {
        ProfileActivityVo activity = new ProfileActivityVo();
        activity.setUserId(userId);
        activity.setOwner(includePrivate);
        activity.setSolvedCount(profileMapper.countSolved(userId));
        activity.setAttemptedCount(profileMapper.countAttempted(userId));

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
        List<ProfileSolutionVo> solutions =
                profileMapper.selectSolutions(userId, includePrivate, MAX_SOLUTIONS + 1);
        activity.setSolutionsTruncated(solutions.size() > MAX_SOLUTIONS);
        activity.setSolutions(solutions.stream().limit(MAX_SOLUTIONS).collect(Collectors.toList()));
        return activity;
    }
}
