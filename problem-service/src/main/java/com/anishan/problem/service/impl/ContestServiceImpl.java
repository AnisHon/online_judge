package com.anishan.problem.service.impl;

import com.anishan.problem.domain.entity.UserContestRelation;
import com.anishan.problem.service.UserContestService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.service.ContestService;
import com.anishan.problem.mapper.ContestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;

/**
* @author happy
* @description 针对表【contest(比赛表)】的数据库操作Service实现
* @createDate 2024-10-16 22:39:15
*/
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ContestServiceImpl extends ServiceImpl<ContestMapper, Contest>
    implements ContestService{

    private final UserContestService userContestService;

    @Override
    public boolean isUserJoined(Long contestId, Long userId) {
        return userContestService.exists(new LambdaQueryWrapper<UserContestRelation>()
                .eq(UserContestRelation::getUserId, userId)
                .eq(UserContestRelation::getContestId, contestId)
        );
    }

    @Override
    public boolean isContestEnable(Long contestId) {
        Contest contest = this.getById(contestId);
        if (contest == null) {
            return false;
        }

        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime startTime = contest.getStartTime();
        LocalDateTime endTime = contest.getEndTime();

        return currentTime.isAfter(startTime) && currentTime.isBefore(endTime);
    }
}




