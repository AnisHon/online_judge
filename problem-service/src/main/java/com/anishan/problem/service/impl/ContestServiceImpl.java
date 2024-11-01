package com.anishan.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.problem.domain.dto.ContestDto;
import com.anishan.problem.domain.entity.UserContestRelation;
import com.anishan.problem.domain.vo.ContestVo;
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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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
    public ContestVo getContestById(Long id) {
        Contest contest = this.getById(id);
        return BeanUtil.copyProperties(contest, ContestVo.class);
    }

    @Override
    public List<ContestVo> listContestById(List<Long> ids) {
        List<Contest> contests = this.listByIds(ids);
        return BeanUtil.copyToList(contests, ContestVo.class);
    }

    @Override
    public PagedResult<ContestVo> listContests(PagedQuery<Contest> pagedQuery) {
        Page<ContestVo> page = pagedQuery.customPage();
        MPJLambdaWrapper<Contest> wrapper = new MPJLambdaWrapper<Contest>()
                .selectCount(UserContestRelation::getUserId, ContestVo::getJoinedNumber)
                .select(
                        Contest::getContestId,
                        Contest::getTitle,
                        Contest::getCreateTime,
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
}




