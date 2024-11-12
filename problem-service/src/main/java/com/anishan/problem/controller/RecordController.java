package com.anishan.problem.controller;

import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.commons.domain.R;
import com.anishan.commons.e.JudgeResult;
import com.anishan.commons.e.ProblemType;
import com.anishan.problem.domain.dto.JudgeRequest;
import com.anishan.problem.domain.dto.UserAnswerRequest;
import com.anishan.problem.domain.entity.Problem;
import com.anishan.problem.domain.entity.Records;
import com.anishan.problem.domain.vo.ContestVo;
import com.anishan.problem.domain.vo.ProblemStatistic;
import com.anishan.problem.domain.vo.ScoredUser;
import com.anishan.problem.domain.vo.UserAnswer;
import com.anishan.problem.service.ContestService;
import com.anishan.problem.service.JudgeService;
import com.anishan.problem.service.RecordsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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


    @PostMapping("/save")
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

    @PostMapping("/get")
    @ApiOperation("获取数据，用于题目答案回写")
    public R<UserAnswer> get(
            @RequestHeader("user-id") Long userId,
            @RequestBody @Validated UserAnswerRequest userAnswerRequest) {

        Records records = recordsService.getOne(new LambdaQueryWrapper<Records>()
                .eq(Records::getUserId, userId)
                .eq(userAnswerRequest.getContestId() != null, Records::getContestId, userAnswerRequest.getContestId())
                .isNull(userAnswerRequest.getContestId() == null, Records::getContestId)
                .eq(Records::getProblemId, userAnswerRequest.getProblemId())
        );

        if (records == null || records.getAnswer() == null) {
            return R.success(null);
        }

        return R.success(records.getAnswer());
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


    @GetMapping("/rank/{contestId}")
    @PreAuthorize("hasAuthority('problem:contest:rank')")
    @ApiOperation("排名")
    public R<List<ScoredUser>> rank(@PathVariable("contestId") Long contestId) {
        List<ScoredUser> users = recordsService.rank(contestId);
        return R.success(users);
    }


    @GetMapping("/statistic/{contestId}")
    @PreAuthorize("hasAuthority('problem:contest:statistic')")
    @ApiOperation("统计题目对错情况")
    public R<List<ProblemStatistic>> statistic(@PathVariable("contestId") Long contestId) {
        List<ProblemStatistic> statistic = recordsService.statistic(contestId);
        return R.success(statistic);
    }
}
