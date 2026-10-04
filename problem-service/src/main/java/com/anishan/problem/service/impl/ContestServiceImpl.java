package com.anishan.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.anishan.commons.domain.R;
import com.anishan.api.client.user.client.ClassClient;
import com.anishan.api.client.user.client.UserClient;
import com.anishan.api.client.user.domain.vo.UserVo;
import com.anishan.api.util.NikeNameUtil;
import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.enumeration.ContestType;
import com.anishan.commons.util.ThrowUtil;
import com.anishan.problem.domain.dto.ContestDto;
import com.anishan.problem.domain.dto.ContestAdminQuery;
import com.anishan.problem.domain.dto.ContestJoinRequest;
import com.anishan.problem.domain.entity.*;
import com.anishan.problem.domain.vo.ContestJoinResponse;
import com.anishan.problem.domain.vo.ContestVo;
import com.anishan.problem.domain.vo.ProblemInListVo;
import com.anishan.problem.domain.vo.SupplementContestVo;
import com.anishan.problem.mapper.ContestMapper;
import com.anishan.problem.mapper.ContestProblemSnapshotMapper;
import com.anishan.problem.mapper.ProblemListMapper;
import com.anishan.problem.service.ContestService;
import com.anishan.problem.service.ContestParticipationService;
import com.anishan.problem.service.ContestProblemSnapshotService;
import com.anishan.problem.service.ContestScheduleService;
import com.anishan.problem.service.ContestFinalRankService;
import com.anishan.problem.service.UserContestService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/**
* @author happy
* @description 针对表【contest(比赛表)】的数据库操作Service实现
* @createDate 2024-10-16 22:39:15
*/
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CacheConfig(cacheNames = "problem:contest:")
public class ContestServiceImpl extends ServiceImpl<ContestMapper, Contest>
    implements ContestService{

    private final UserContestService userContestService;
    private final ContestMapper contestMapper;
    private final ProblemListMapper problemListMapper;
    private final ContestProblemSnapshotMapper snapshotMapper;
    private final ContestProblemSnapshotService snapshotService;
    private final UserClient userClient;
    private final ClassClient classClient;
    private final ContestParticipationService participationService;
    private final ContestScheduleService scheduleService;
    private final ContestFinalRankService finalRankService;

    @Override
    public LocalDateTime getTime(long id) {
        return this.getObj(
                new LambdaQueryWrapper<Contest>()
                        .select(Contest::getUpdateTime)
                        .eq(Contest::getContestId, id),
                x -> (LocalDateTime) x
        );
    }

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

    @Override
    @Cacheable(key = "#id")
    public ContestVo getContestById(Long id) {
        Contest contest = this.getOne(
                new LambdaQueryWrapper<Contest>()
                        .select(
                                Contest::getContestId, Contest::getUserId, Contest::getTitle, Contest::getListId,
                                Contest::getDescription, Contest::getAuth, Contest::getStartTime, Contest::getEndTime,
                                Contest::getType
                        )
                        .eq(Contest::getContestId, id)
        );
        return BeanUtil.copyProperties(contest, ContestVo.class);
    }

    @Override
    public List<ContestVo> listContestById(List<Long> ids) {
        List<Contest> contests = this.listByIds(ids);
        return BeanUtil.copyToList(contests, ContestVo.class);
    }

    @Override
    public PagedResult<ContestVo> listContests(PagedQuery<Contest> pagedQuery, ContestType type) {
        Page<ContestVo> page = pagedQuery.customPage();
        MPJLambdaWrapper<Contest> wrapper = new MPJLambdaWrapper<Contest>()
                .selectCount(UserContestRelation::getUserId, ContestVo::getJoinedNumber)
                .select(
                        Contest::getContestId,
                        Contest::getTitle,
                        Contest::getStartTime,
                        Contest::getEndTime,
                        Contest::getAuth
                )
                .leftJoin(UserContestRelation.class, UserContestRelation::getContestId, Contest::getContestId)
                .eq(Contest::getType, type)
                .groupBy(Contest::getContestId)
                .orderByDesc(Contest::getStartTime);


        page = contestMapper.selectJoinPage(page, ContestVo.class, wrapper);

        return PagedResult.build(page);
    }

    @Override
    @CacheEvict(key = "#contestDto.contestId")
    public boolean updateContest(ContestDto contestDto) {
        return scheduleService.update(contestDto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addContest(ContestDto classDto) {
        validateSchedule(classDto);
        Contest contest = BeanUtil.copyProperties(classDto, Contest.class);
        contest.setScoringVersion(2);
        contest.setNextAttemptSeq(0L);
        boolean saved = this.save(contest);
        if (saved && contest.getType() == ContestType.CONTEST) {
            finalRankService.initializeForContest(contest.getContestId());
        }
        return saved;
    }

    @Override
    public PagedResult<ContestVo> listContestsAdmin(ContestAdminQuery pagedQuery, ContestType type) {
        Page<ContestVo> page = pagedQuery.customPage();
        LocalDateTime now = LocalDateTime.now();
        MPJLambdaWrapper<Contest> wrapper = new MPJLambdaWrapper<Contest>()
                .selectCount(UserContestRelation::getUserId, ContestVo::getJoinedNumber)
                .selectAll(Contest.class)
                .leftJoin(UserContestRelation.class, UserContestRelation::getContestId, Contest::getContestId)
                .eq(Contest::getType, type)
                .groupBy(Contest::getContestId)
                .orderByDesc(Contest::getStartTime);

        if (StringUtils.hasText(pagedQuery.getKeyword())) {
            String keyword = pagedQuery.getKeyword().trim();
            wrapper.and(query -> query
                    .like(Contest::getTitle, keyword)
                    .or()
                    .like(Contest::getDescription, keyword));
        }

        if (StringUtils.hasText(pagedQuery.getStatus())) {
            switch (pagedQuery.getStatus().trim().toLowerCase()) {
                case "pending":
                    wrapper.gt(Contest::getStartTime, now);
                    break;
                case "running":
                    wrapper.le(Contest::getStartTime, now)
                            .ge(Contest::getEndTime, now);
                    break;
                case "ended":
                    wrapper.lt(Contest::getEndTime, now);
                    break;
                default:
                    break;
            }
        }


        page = contestMapper.selectJoinPage(page, ContestVo.class, wrapper);

        return PagedResult.build(page);
    }

    private void validateSchedule(ContestDto contestDto) {
        ThrowUtil.businessError(contestDto.getStartTime() == null || contestDto.getEndTime() == null,
                "开始时间和结束时间不能为空");
        ThrowUtil.businessError(!contestDto.getStartTime().isBefore(contestDto.getEndTime()),
                "结束时间必须晚于开始时间");
        ThrowUtil.businessError(contestDto.getAuth() == null, "访问策略不能为空");
        if (contestDto.getAuth().name().equalsIgnoreCase("PRIVATE")) {
            ThrowUtil.businessError(!StringUtils.hasText(contestDto.getPwd()), "私有活动必须设置访问密码");
        }
    }

    @Override
    public ContestJoinResponse joinContest(Long userId, ContestJoinRequest req) {
        return participationService.join(userId, req);
    }

    @Override
    public List<ProblemInListVo> listProblemInContest(Long userId, @NotNull Long contestId) {
        boolean b = isUserJoined(contestId, userId);
        ThrowUtil.permissionDeny(!b , "您无权访问");
        snapshotService.createAfterStart(contestId);
        return problemListMapper.selectContestSnapshotProblems(contestId, userId);
    }


    @Override
    public BigDecimal getScore(Long contestId, Long ProblemId) {
        if (contestId == null || ProblemId == null) {
            return BigDecimal.ZERO;
        }

        return snapshotMapper.selectMaxScore(contestId, ProblemId);
    }

    @Override
    public boolean getStatus(Long userId, Long contestId) {
        return participationService.canUserSubmit(contestId, userId);
    }

    @Override
    public boolean addLateSubmission(SupplementContest supplementContest) {
        return participationService.setSupplement(supplementContest);
    }

    @Override
    public List<SupplementContestVo> getLateSubmission(Long contestId) {
        List<SupplementContest> supplementContests = Db.list(
                Wrappers
                        .lambdaQuery(SupplementContest.class)
                        .eq(SupplementContest::getContestId, contestId)
        );


        List<SupplementContestVo> supplementContestVoes =
                BeanUtil.copyToList(supplementContests, SupplementContestVo.class);


        NikeNameUtil.setNikeName(supplementContestVoes);

        return supplementContestVoes;
    }

    @Override
    public List<UserVo> getJoinedUser(Long contestId) {
        UserContestRelation userContestRelation = new UserContestRelation();
        userContestRelation.setContestId(contestId);

        List<Long> userIds = Db
                .list(userContestRelation)
                .stream()
                .map(UserContestRelation::getUserId)
                .collect(Collectors.toList());

        if (CollUtil.isEmpty(userIds)) {
            return ListUtil.empty();
        }

        return userClient.listUser(userIds).getData();
    }

    @Override
    public boolean removeUser(Long contestId, List<Long> userIds) {
        return participationService.removeUsers(contestId, userIds);
    }

    @Override
    public boolean addUserByClass(Long contestId, Long classIds) {
        if (classIds == null || classIds <= 0) {
            throw new com.anishan.commons.exception.ApiStatusException(400, "班级ID无效");
        }
        R<List<UserVo>> response = classClient.listUser(classIds);
        if (response == null || response.getCode() != 200 || response.getData() == null) {
            throw new com.anishan.commons.exception.ApiStatusException(503, "无法获取班级成员，请稍后重试");
        }
        if (response.getData().size() > ContestParticipationService.MAX_ROSTER_BATCH_SIZE) {
            throw new com.anishan.commons.exception.ApiStatusException(400, "班级成员超过单次调整上限，请缩小班级范围");
        }
        List<Long> userIds = response.getData().stream().map(UserVo::getUserId).collect(Collectors.toList());
        return participationService.addUsers(contestId, userIds);
    }

    @Override
    public boolean addUser(Long contestId, List<Long> userIds) {
        return participationService.addUsers(contestId, userIds);
    }
}
