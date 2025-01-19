package com.anishan.problem.service;

import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.problem.domain.dto.ContestDto;
import com.anishan.problem.domain.dto.ContestJoinRequest;
import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.domain.entity.SupplementContest;
import com.anishan.problem.domain.vo.ContestJoinResponse;
import com.anishan.problem.domain.vo.ContestVo;
import com.anishan.problem.domain.vo.ProblemInListVo;
import com.anishan.problem.domain.vo.SupplementContestVo;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
* @author happy
* @description 针对表【contest(比赛表)】的数据库操作Service
* @createDate 2024-10-16 22:39:16
*/
public interface ContestService extends IService<Contest> {

    LocalDateTime getTime(long id);

    boolean isUserJoined(Long contestId, Long userId);

    boolean isContestEnable(Long contestId);

    ContestVo getContestById(@NotNull(message = "id为Null") Long id);

    List<ContestVo> listContestById(List<Long> ids);

    PagedResult<ContestVo> listContests(PagedQuery<Contest> pagedQuery);

    boolean updateContest(ContestDto contestDto);

    boolean addContest(ContestDto contestDto);

    PagedResult<ContestVo> listContestsAdmin(PagedQuery<Contest> pagedQuery);

    ContestJoinResponse joinContest(Long userId, ContestJoinRequest contestJoinRequest);

    List<ProblemInListVo> listProblemInContest(Long userId, @NotNull Long contestId);

    BigDecimal getScore(Long contestId, Long ProblemId);

    boolean getStatus(Long userId, String contestId);

    boolean addLateSubmission(SupplementContest supplementContest);

    List<SupplementContestVo> getLateSubmission(Long contestId);
}
