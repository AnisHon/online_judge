package com.anishan.problem.controller;

import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.commons.domain.R;
import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.commons.enumeration.ProblemType;
import com.anishan.problem.domain.dto.JudgeRequest;
import com.anishan.problem.domain.dto.UserAnswerRequest;
import com.anishan.problem.domain.entity.Problem;
import com.anishan.problem.domain.entity.Records;
import com.anishan.problem.domain.entity.UserContestRelation;
import com.anishan.problem.domain.vo.*;
import com.anishan.problem.service.ContestService;
import com.anishan.problem.service.JudgeService;
import com.anishan.problem.service.RecordsService;
import com.anishan.problem.service.UserContestService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Api("负责题目数据回写的接口")
@RestController
@RequestMapping("/record")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RecordController {

    private final RecordsService recordsService;
    private final JudgeService judgeService;
    private final ContestService contestService;
    private final UserContestService userContestService;

    @PostMapping("judge-save")
    @ApiOperation("内部接口，保存judge数据")
    public void judgeSave(@RequestBody JudgeScore judgeScore) {

        Records records = new Records()
                .setContestId(judgeScore.getContestId())
                .setProblemId(judgeScore.getProblemId())
                .setUserId(judgeScore.getUserId())
                .setStatus(judgeScore.getResult() == JudgeResult.Accept)
                .setScore(judgeScore.getScore())
                .setAnswer(new UserAnswer(null, judgeScore.getCode(), judgeScore.getLanguageId()));
        recordsService.addRecord(records);
    }


    @PostMapping
    @ApiOperation("保存数据")
    public R<Boolean> save(
            @RequestHeader("user-id") Long userId,
            @RequestBody @Validated JudgeRequest judgeRequest
    ) {
        Long problemId = judgeRequest.getProblemId();

        ProblemType type = Db.getById(problemId, Problem.class).getType();


//        选择题填空直接判
        if (type != ProblemType.OJ) {
            judgeService.judge(userId, judgeRequest);
            return R.success(true);
        } else  {
            Records records = new Records(
                    null,
                    judgeRequest.getContestId(),
                    userId,
                    problemId,
                    null,
                    false,
                    new UserAnswer(judgeRequest.getAnswers(), judgeRequest.getCode(), judgeRequest.getLanguageId())
            );
            boolean b = recordsService.addRecord(records);
            return R.success(b);
        }
    }

    @GetMapping
    @ApiOperation("获取数据，用于题目答案回写")
    public R<UserAnswer> get(
            @RequestHeader("user-id") Long userId,
            @Validated UserAnswerRequest userAnswerRequest) {

        UserAnswer userAnswer = recordsService.getAnswer(userId, userAnswerRequest);

        return R.success(userAnswer);
    }

    @GetMapping("/score/{contestId}")
    @ApiOperation("获取分数")
    public R<BigDecimal> score(@PathVariable("contestId") Long contestId, @RequestHeader("user-id") Long userId) {
        ContestVo contest = contestService.getContestById(contestId);

        if (LocalDateTime.now().isBefore(contest.getEndTime())) {
            return R.success(null);
        }

        BigDecimal score = recordsService.score(contestId, userId);
        return R.success(score);
    }


    @GetMapping("/statistic/user/{contestId}")
    @PreAuthorize("hasAuthority('problem:contest:statistic')")
    @ApiOperation("用户分数对错统计")
    public R<List<UserStatistic>> userStatistic(@ApiParam("比赛ID") @PathVariable("contestId") Long contestId) {
        List<UserStatistic> users = recordsService.getUserStatistic(contestId);
        return R.success(users);
    }


    @GetMapping("/statistic/problem/{contestId}")
    @PreAuthorize("hasAuthority('problem:contest:statistic')")
    @ApiOperation("统计题目对错情况")
    public R<List<ProblemStatistic>> problemStatistic(@ApiParam("比赛ID") @PathVariable("contestId") Long contestId) {
        List<ProblemStatistic> statistic = recordsService.getProblemStatistic(contestId);
        return R.success(statistic);
    }

    @GetMapping("/score/problem")
    @PreAuthorize("hasAuthority('problem:contest:statistic')")
    @ApiOperation("查看比赛的题目分数")
    public R<List<ProblemScore>> problemScores(
            @ApiParam("题目ID") @NotNull Long problemId,
            @ApiParam("比赛ID") @NotNull Long contestId
    ) {
        List<ProblemScore> statistic = recordsService.getProblemScores(problemId, contestId);
        return R.success(statistic);
    }

    @GetMapping("/score/user")
    @PreAuthorize("hasAuthority('problem:contest:statistic')")
    @ApiOperation("查看比赛的用户分数")
    public R<List<UserScore>> userScores(
            @ApiParam("用户ID") @NotNull Long userId,
            @ApiParam("比赛ID") @NotNull Long contestId
    ) {
        List<UserScore> statistic = recordsService.getUserScores(userId, contestId);
        return R.success(statistic);
    }

    @GetMapping("/admin/answer")
    @ApiOperation("管理员获取用户答案")
    @PreAuthorize("hasAuthority('problem:contest:statistic')")
    public R<UserAnswer> getAdminAnwer(
            @NotNull Long userId,
            @NotNull Long contestId,
            @NotNull Long problemId
    ) {
        UserAnswerRequest userAnswerRequest = new UserAnswerRequest();
        userAnswerRequest.setContestId(contestId);
        userAnswerRequest.setProblemId(problemId);

        UserAnswer userAnswer = recordsService.getAnswer(userId, userAnswerRequest);
        return R.success(userAnswer);
    }

    @GetMapping("/join-number/{contestId}")
    @ApiOperation("参加的人数")
    public R<Long> joinNumber(@PathVariable("contestId") Long contestId) {
        long count = userContestService
                .count(
                        new LambdaQueryWrapper<UserContestRelation>()
                                .eq(UserContestRelation::getContestId, contestId)
                );
        return R.success(count);
    }
}
