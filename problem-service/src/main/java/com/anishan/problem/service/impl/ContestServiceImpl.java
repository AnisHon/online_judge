package com.anishan.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.anishan.api.client.user.client.ClassClient;
import com.anishan.api.client.user.client.UserClient;
import com.anishan.api.client.user.domain.vo.UserVo;
import com.anishan.api.util.NikeNameUtil;
import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.enumeration.ContestType;
import com.anishan.commons.util.ThrowUtil;
import com.anishan.problem.domain.dto.ContestDto;
import com.anishan.problem.domain.dto.ContestJoinRequest;
import com.anishan.problem.domain.entity.*;
import com.anishan.problem.domain.vo.ContestJoinResponse;
import com.anishan.problem.domain.vo.ContestVo;
import com.anishan.problem.domain.vo.ProblemInListVo;
import com.anishan.problem.domain.vo.SupplementContestVo;
import com.anishan.problem.mapper.ContestMapper;
import com.anishan.problem.service.ContestService;
import com.anishan.problem.service.ProblemListService;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private final ProblemListService problemListService;
    private final UserClient userClient;
    private final ClassClient classClient;

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
                                Contest::getDescription, Contest::getAuth, Contest::getStartTime, Contest::getEndTime
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
        Contest contest = BeanUtil.copyProperties(contestDto, Contest.class);

        LocalDateTime time = this.getTime(contestDto.getContestId());
        contest.setUpdateTime(time);

        return this.updateById(contest);
    }

    @Override
    public boolean addContest(ContestDto classDto) {
        Contest contest = BeanUtil.copyProperties(classDto, Contest.class);
        return this.save(contest);
    }

    @Override
    public PagedResult<ContestVo> listContestsAdmin(PagedQuery<Contest> pagedQuery, ContestType type) {
        Page<ContestVo> page = pagedQuery.customPage();
        MPJLambdaWrapper<Contest> wrapper = new MPJLambdaWrapper<Contest>()
                .selectCount(UserContestRelation::getUserId, ContestVo::getJoinedNumber)
                .selectAll(Contest.class)
                .leftJoin(UserContestRelation.class, UserContestRelation::getContestId, Contest::getContestId)
                .eq(Contest::getType, type)
                .groupBy(Contest::getContestId)
                .orderByDesc(Contest::getStartTime);


        page = contestMapper.selectJoinPage(page, ContestVo.class, wrapper);

        return PagedResult.build(page);
    }

    private boolean isInContesting(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(startTime) && now.isBefore(endTime);
    }

    private ContestJoinResponse joinContestCheck(ContestJoinRequest req) {
        Contest contest = this.getById(req.getContestId());
        if (contest == null) {
            return ContestJoinResponse.fail("比赛不存在");
        } else if (!isInContesting(contest.getStartTime(), contest.getEndTime())) {
            return ContestJoinResponse.fail("比赛目前不可加入");
        }



        ContestJoinResponse resp = null;

        switch (contest.getAuth()) {
            case PUBLIC:
                resp = ContestJoinResponse.success();
                break;
            case PRIVATE:
                boolean equals = Objects.equals(contest.getPwd(), req.getPassword());
                resp = ContestJoinResponse.conditional(equals, "密码错误");
                break;
            case WhiteList:
                resp = ContestJoinResponse.fail("无法加入");
                break;
        }

        return resp;
    }


    @Override
    public ContestJoinResponse joinContest(Long userId, ContestJoinRequest req) {

        ContestJoinResponse response = joinContestCheck(req);
        if (!response.isSuccess()) {
            return response;
        }

        try {
            userContestService.save(new UserContestRelation(userId, req.getContestId()));
        } catch (DuplicateKeyException ignore) {
            response.setSuccess(false);
            response.setMessage("已经加入了");
        }

        return response;
    }

    @Override
    public List<ProblemInListVo> listProblemInContest(Long userId, @NotNull Long contestId) {
        boolean b = isUserJoined(contestId, userId);
        ThrowUtil.permissionDeny(!b , "您无权访问");


        Long listId = this.getObj(
                new LambdaQueryWrapper<Contest>()
                        .select(Contest::getListId)
                        .eq(Contest::getContestId, contestId),
                x -> (Long) x
        );

        return problemListService.getProblemsForUser(listId, contestId);
    }


    @Override
    public BigDecimal getScore(Long contestId, Long ProblemId) {
        if (contestId == null || ProblemId == null) {
            return BigDecimal.ZERO;
        }

        MPJLambdaWrapper<Contest> wrapper = new MPJLambdaWrapper<Contest>()
                .selectAll(ProblemProblemListRelation.class)
                .leftJoin(ProblemProblemListRelation.class, ProblemProblemListRelation::getListId, Contest::getListId)
                .eq(Contest::getContestId, contestId)
                .eq(ProblemProblemListRelation::getProblemId, ProblemId);

        ProblemProblemListRelation relation = contestMapper.selectJoinOne(ProblemProblemListRelation.class, wrapper);
        return relation.getScore();
    }

    @Override
    public boolean getStatus(Long userId, Long contestId) {
        LocalDateTime now = LocalDateTimeUtil.now();

        boolean submitted = Db.count(Wrappers.lambdaQuery(UserSubmit.class)
                .eq(UserSubmit::getUserId, userId)
                .eq(UserSubmit::getContestId, contestId)) > 0;

        Contest contest = this.getById(contestId);
        SupplementContest supplementContest = Db.getOne(
                Wrappers.lambdaQuery(SupplementContest.class)
                        .eq(SupplementContest::getContestId, contestId)
                        .eq(SupplementContest::getUserId, userId)
        );
        boolean joined = isUserJoined(contestId, userId);
        // 没参加
        if (!joined) {
            return false;
        }

        if (submitted) {
            return false;
        }

        // 当前日期是否早于结束日期
        boolean status = now.isBefore(contest.getEndTime());


        // 如果有补交当前时间是否早于supplement的deadline
        if (supplementContest != null) {
            status = now.isBefore(supplementContest.getDeadline());
        }

        return status;
    }

    @Override
    @Transactional
    public boolean addLateSubmission(SupplementContest supplementContest) {

        Contest contest = this.getById(supplementContest.getContestId());

        boolean joined = isUserJoined(contest.getContestId(), supplementContest.getUserId());
        LocalDateTime endTime = contest.getEndTime();
        LocalDateTime deadline = supplementContest.getDeadline();

        ThrowUtil.businessError(contest.getType() == ContestType.CONTEST, "比赛不支持补交");
        ThrowUtil.businessError(deadline.isBefore(endTime), "最迟时间不能早于结束时间");
        ThrowUtil.businessError(!joined, "用户未参加比赛");

        return Db.save(supplementContest);
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
    @Transactional
    public boolean removeUser(Long contestId, List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return false;
        }


        Db.remove(
                Wrappers.lambdaQuery(ContestRecords.class)
                        .eq(ContestRecords::getContestId, contestId)
                        .in(ContestRecords::getUserId, userIds)
        );

        return userContestService.remove(
                Wrappers.lambdaQuery(UserContestRelation.class)
                        .eq(UserContestRelation::getContestId, contestId)
                        .in(UserContestRelation::getUserId, userIds)
        );
    }

    @Override
    public boolean addUserByClass(Long contestId, Long classIds) {
        List<UserContestRelation> relations = classClient.listUser(classIds).getData().stream()
                .map(UserVo::getUserId)
                .map(userId -> new UserContestRelation(userId, contestId))
                .collect(Collectors.toList());


        return userContestService.saveIgnore(relations);
    }
}




