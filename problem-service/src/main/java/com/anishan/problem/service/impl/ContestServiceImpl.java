package com.anishan.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.anishan.api.annotation.EnableCache;
import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.util.ThrowUtil;
import com.anishan.problem.domain.dto.ContestDto;
import com.anishan.problem.domain.dto.ContestJoinRequest;
import com.anishan.problem.domain.entity.ProblemProblemListRelation;
import com.anishan.problem.domain.entity.UserContestRelation;
import com.anishan.problem.domain.vo.ContestJoinResponse;
import com.anishan.problem.domain.vo.ContestVo;
import com.anishan.problem.domain.vo.ProblemInListVo;
import com.anishan.problem.service.ProblemListService;
import com.anishan.problem.service.UserContestService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.service.ContestService;
import com.anishan.problem.mapper.ContestMapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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
    private final ContestMapper contestMapper;
    private final ProblemListService problemListService;

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
    @EnableCache(name = "get-contest-id")
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
    @EnableCache(name = "list-contests", expire = 30 * 1000)
    public PagedResult<ContestVo> listContests(PagedQuery<Contest> pagedQuery) {
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
                .groupBy(Contest::getContestId)
                .orderByDesc(Contest::getStartTime);


        page = contestMapper.selectJoinPage(page, ContestVo.class, wrapper);

        return PagedResult.build(page);
    }

    @Override
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
    public PagedResult<ContestVo> listContestsAdmin(PagedQuery<Contest> pagedQuery) {
        Page<ContestVo> page = pagedQuery.customPage();
        MPJLambdaWrapper<Contest> wrapper = new MPJLambdaWrapper<Contest>()
                .selectCount(UserContestRelation::getUserId, ContestVo::getJoinedNumber)
                .selectAll(Contest.class)
                .leftJoin(UserContestRelation.class, UserContestRelation::getContestId, Contest::getContestId)
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

        return problemListService.getProblemsForUser(listId);
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
}




