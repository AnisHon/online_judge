package com.anishan.problem.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.anishan.api.client.gojudge.domain.TestResult;
import com.anishan.api.client.judgeserver.client.JudgeClient;
import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.api.client.judgeserver.domain.RunTestInfo;
import com.anishan.api.util.RedisJudgeTestUtil;
import com.anishan.api.util.RedisJudgeSubmissionLock;
import com.anishan.problem.domain.dto.TestRequest;
import com.anishan.commons.enumeration.ProblemType;
import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.commons.util.ThrowUtil;
import com.anishan.problem.domain.dto.JudgeAnswer;
import com.anishan.problem.domain.ScoreAndIsCorrected;
import com.anishan.problem.domain.dto.JudgeRequest;
import com.anishan.problem.domain.entity.*;
import com.anishan.problem.domain.vo.ProblemJudgeResult;
import com.anishan.problem.domain.vo.UserAnswer;
import com.anishan.problem.service.*;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JudgeServiceImpl implements JudgeService {

    private final ProblemService problemService;
    private final ContestService contestService;
    private final RecordsService recordsService;
    private final OjProblemService ojProblemService;
    private final ChoiceFillAnswersService choiceFillAnswersService;
    private final SysLanguageService sysLanguageService;
    private final RedisJudgeTestUtil redisJudgeTestUtil;
    private final JudgeClient judgeClient;
    private final SubmitLogService submitLogService;
    private final RedisJudgeSubmissionLock redisJudgeSubmissionLock;


    private ProblemJudgeResult judgeOj(Long userId, Problem problem, JudgeRequest judgeRequest) {

        // 获取OJ题目
        OjProblem ojProblem = ojProblemService.getById(judgeRequest.getProblemId());
        ThrowUtil.runtime(ojProblem == null, "题目被删除或不存在");

        // 获取语言
        String languageName = sysLanguageService.getNameById(judgeRequest.getLanguageId());
        ThrowUtil.runtime(languageName == null, "不支持的语言");

        ProblemJudgeResult problemJudgeResult = new ProblemJudgeResult();
//        problemJudgeResult.setSubmitId(submitId);


        BigDecimal score = null;
        if (judgeRequest.getContestId() != null) {
            score = contestService.getScore(judgeRequest.getContestId(), problem.getProblemId());
            // 竞赛题必须真实属于该竞赛，不能仅凭请求中的 contestId 进入竞赛判题链路。
            ThrowUtil.businessError(score == null, "题目不属于指定比赛");
        }

        // 校验通过后再加锁，避免无效请求占用用户的判题名额。
        String submissionLockToken = redisJudgeSubmissionLock.tryAcquire(userId);
        ThrowUtil.businessError(submissionLockToken == null, "上一条提交正在判题，请等待结果后再提交");

        JudgeInfo info = new JudgeInfo()
                .setUserId(userId)
                .setSubmissionLockToken(submissionLockToken)
                .setProblemId(problem.getProblemId())
                .setContestId(judgeRequest.getContestId())
                .setLanguageId(judgeRequest.getLanguageId())
                .setCode(judgeRequest.getCode())
                .setLanguage(languageName)
                .setTimeLimit(ojProblem.getTimeLimit())
                .setMemoryLimit(ojProblem.getMemoryLimit())
                .setStackLimit(ojProblem.getStackLimit())
                .setListScore(score);

        Long submitId = null;
        boolean dispatched = false;
        Boolean dispatchAccepted = null;
        try {
            // 先建立公开提交记录，再把提交 ID 传入判题机，保证异步链路有稳定主键。
            submitId = submitLogService.createQueued(info);
            info.setSubmitId(submitId);

            dispatchAccepted = judgeClient.judge(info).getData();
            // 成功投递后保留锁，等 judgeResult 异步回调释放。
            dispatched = Boolean.TRUE.equals(dispatchAccepted);
        } catch (Exception e) {
            // Feign/MQ 不可用时不能留下永久 QUEUE，内部原因落库，用户只看到通用提示。
            if (submitId != null) {
                submitLogService.complete(new JudgeScore()
                        .setSubmitId(submitId)
                        .setUserId(userId)
                        .setProblemId(problem.getProblemId())
                        .setContestId(judgeRequest.getContestId())
                        .setCode(judgeRequest.getCode())
                        .setResult(JudgeResult.JUDGE_ERROR)
                        .setErrorCode("DISPATCH_EXCEPTION")
                        .setInternalError(e.getClass().getName() + ": " + e.getMessage()));
            }
            ThrowUtil.businessError(true, "判题服务暂时不可用，请稍后重试");
            return problemJudgeResult;
        } finally {
            // 成功投递后由 /internal/judgeResult 释放；其余路径立即释放。
            // Redis 本身仍保留 12 秒硬过期，覆盖进程崩溃和回调丢失。
            if (!dispatched) {
                redisJudgeSubmissionLock.release(userId, submissionLockToken);
            }
        }

        if (!Boolean.TRUE.equals(dispatchAccepted)) {
            submitLogService.complete(new JudgeScore()
                    .setSubmitId(submitId)
                    .setUserId(userId)
                    .setProblemId(problem.getProblemId())
                    .setContestId(judgeRequest.getContestId())
                    .setCode(judgeRequest.getCode())
                    .setResult(JudgeResult.JUDGE_ERROR)
                    .setErrorCode("DISPATCH_REJECTED")
                    .setInternalError("judge-server rejected the submission"));
            ThrowUtil.businessError(true, "判题服务繁忙，请稍后提交");
        }

        problemJudgeResult.setSubmitId(submitId);


        return problemJudgeResult;

    }

    /**
     * 判断是否正确，正确给分，否则0分，用于填空题
     * @param judgeAnswer 用户输入的答案
     * @param blankAnswers 标准答案
     * @return 分数和是否正确
     */
    private ScoreAndIsCorrected judgeScore(
            JudgeAnswer judgeAnswer,
            Map<Integer, List<ChoiceFillAnswers>> blankAnswers
    ) {
        Integer index = judgeAnswer.getIndex();
        // 没有对应答案直接0分
        if (!blankAnswers.containsKey(index)) {
            return ScoreAndIsCorrected.wrong();
        }

        // 去除首尾空格
        String userAnswers = judgeAnswer.getAnswer().trim();

        // 判断多个答案中是否有一样的
        List<ChoiceFillAnswers> choiceFillAnswers = blankAnswers.get(index);
        for (ChoiceFillAnswers choiceFillAnswer : choiceFillAnswers) {
            String answerText = choiceFillAnswer.getAnswerText();
            if (Objects.equals(answerText, userAnswers)) {
                return ScoreAndIsCorrected.corrected(choiceFillAnswer.getScore());
            }
        }
        return ScoreAndIsCorrected.wrong();
    }

    /**
     * 判断是否正确，正确给分，否则0分，用于选择题
     * @param judgeAnswer 用户输入的答案
     * @param choiceFillAnswers 标准答案
     * @return 分数和是否正确
     */
    private ScoreAndIsCorrected judgeScore(
            JudgeAnswer judgeAnswer,
            List<ChoiceFillAnswers> choiceFillAnswers
    ) {
        Integer index = judgeAnswer.getIndex();
//        初始化一个0分结果
        ScoreAndIsCorrected scoreAndIsCorrected = new ScoreAndIsCorrected();
        scoreAndIsCorrected.setScore(BigDecimal.ZERO);
        scoreAndIsCorrected.setCorrected(false);

//        遍历标准答案，存在一样的答案就算对
        for (ChoiceFillAnswers choiceFillAnswer : choiceFillAnswers) {
            if (Objects.equals(choiceFillAnswer.getBlankIndex(), index)) {
                scoreAndIsCorrected.setCorrected(true);
                scoreAndIsCorrected.setScore(choiceFillAnswer.getScore());

                return scoreAndIsCorrected;
            }
        }
        return scoreAndIsCorrected;
    }


    /**
     * 填空题判断是否相等
     * @param blankAnswers 填空标准答案数组
     * @param inputAnswers 用户输入答案数组
     * @return ProblemJudgeResult对象，表示分数，是否正确
     */
    private ProblemJudgeResult fillEquals(
            Map<Integer, List<ChoiceFillAnswers>> blankAnswers,
            List<JudgeAnswer> inputAnswers
    ) {
        ProblemJudgeResult problemJudgeResult = new ProblemJudgeResult();
        problemJudgeResult.setTotalScore(BigDecimal.ZERO);
        problemJudgeResult.setCorrect(inputAnswers.size() == blankAnswers.size());
        for (JudgeAnswer inputAnswer : inputAnswers) {
            ScoreAndIsCorrected judgeResult = judgeScore(inputAnswer, blankAnswers);
            if (judgeResult.isCorrected()) {
                BigDecimal score = problemJudgeResult.getTotalScore().add(judgeResult.getScore());
                problemJudgeResult.setTotalScore(score);
            } else {
                problemJudgeResult.setCorrect(false);
            }
        }
        return problemJudgeResult;
    }

    private ProblemJudgeResult doJudgeFill(List<ChoiceFillAnswers> answer, JudgeRequest judgeRequest) {


            Map<Integer, List<ChoiceFillAnswers>> blankAnswers = answer
                .stream()
                .collect(Collectors.groupingBy(ChoiceFillAnswers::getBlankIndex));

        return fillEquals(blankAnswers, judgeRequest.getAnswers());
    }

    private ProblemJudgeResult judgeFill(Problem problem, JudgeRequest judgeRequest) {
        List<ChoiceFillAnswers> blankAnswers = choiceFillAnswersService.get(problem.getProblemId());


        ProblemJudgeResult judgeResult = doJudgeFill(blankAnswers, judgeRequest);

        BigDecimal fillMark = calcFillFullMark(blankAnswers);
        judgeResult.setFullMark(fillMark);


        judgeResult.setAnswers(blankAnswers.stream().map(x -> new JudgeAnswer(x.getBlankIndex(), x.getAnswerText())).collect(Collectors.toList()));

        return judgeResult;
    }


    private ProblemJudgeResult doJudgeChoice(List<ChoiceFillAnswers> answer, JudgeRequest judgeRequest) {
//        初始化一个空的结果
        ProblemJudgeResult problemJudgeResult = new ProblemJudgeResult();
        problemJudgeResult.setTotalScore(BigDecimal.ZERO);
        problemJudgeResult.setCorrect(false);

//        去重
        judgeRequest.getAnswers().forEach(x -> x.setAnswer(null));
        List<JudgeAnswer> collect = judgeRequest
                .getAnswers()
                .stream()
                .distinct()
                .collect(Collectors.toList());


//        用户答案比标准答案多直接0分
        if (answer.size() < collect.size()) {
            return problemJudgeResult;
        }

//        遍历用户答案
        for (JudgeAnswer judgeRequestAnswer : collect) {
//            判题，查看用户答案是否正确
            ScoreAndIsCorrected scoreAndIsCorrected = judgeScore(judgeRequestAnswer, answer);

//            正确就加分
            if (scoreAndIsCorrected.isCorrected()) {
                problemJudgeResult.add(scoreAndIsCorrected.getScore());
            } else {
//                选择题只要不正确直接0分
                problemJudgeResult.setCorrect(false);
                problemJudgeResult.setTotalScore(BigDecimal.ZERO);
                return problemJudgeResult;
            }
        }

        problemJudgeResult.setCorrect(judgeRequest.getAnswers().size() == answer.size());

        return problemJudgeResult;
    }

    private ProblemJudgeResult judgeChoice(Problem problem, JudgeRequest judgeRequest) {
        List<ChoiceFillAnswers> blankAnswers = choiceFillAnswersService.list(
                new LambdaUpdateWrapper<ChoiceFillAnswers>()
                        .eq(ChoiceFillAnswers::getProblemId, problem.getProblemId())
                        .eq(ChoiceFillAnswers::getIsCorrect, true)
        );

        List<JudgeAnswer> answers = blankAnswers
                .stream()
                .map((x) -> new JudgeAnswer(x.getBlankIndex(), x.getAnswerText()))
                .collect(Collectors.toList());
        ProblemJudgeResult problemJudgeResult = doJudgeChoice(blankAnswers, judgeRequest);
        problemJudgeResult.setAnswers(answers);

        BigDecimal fillMark = calcChoiceFullMark(blankAnswers);
        problemJudgeResult.setFullMark(fillMark);

        return problemJudgeResult;
    }

    private BigDecimal calcFillFullMark(List<ChoiceFillAnswers> answers) {
        List<ChoiceFillAnswers> distinct = CollUtil.distinct(answers, ChoiceFillAnswers::getBlankIndex, false);
        BigDecimal fullMark = BigDecimal.ZERO;
        for (ChoiceFillAnswers choiceFillAnswers : distinct) {
            fullMark = fullMark.add(choiceFillAnswers.getScore());
        }
        return fullMark;
    }

    private BigDecimal calcChoiceFullMark(List<ChoiceFillAnswers> answers) {
        List<ChoiceFillAnswers> collect = CollUtil.distinct(answers, ChoiceFillAnswers::getBlankIndex, false)
                .stream()
                .filter(ChoiceFillAnswers::getIsCorrect)
                .collect(Collectors.toList());

        BigDecimal fullMark = BigDecimal.ZERO;
        for (ChoiceFillAnswers choiceFillAnswers : collect) {
            fullMark = fullMark.add(choiceFillAnswers.getScore());
        }

        return fullMark;
    }


    private ProblemJudgeResult judge(Long userId, Problem problem, JudgeRequest judgeRequest) {
        ProblemType type = problem.getType();
        ProblemJudgeResult judgeResult = null;
        switch (type) {
            case OJ:
                judgeResult = judgeOj(userId, problem, judgeRequest);
                break;
            case FILL:
                judgeResult = judgeFill(problem, judgeRequest);
                break;
            case CHOICE:
            case MULTI_CHOICE:
                judgeResult = judgeChoice(problem, judgeRequest);
                break;
        }



        return judgeResult;
    }


    private void beforeJudgeCheck(Problem problem, JudgeRequest judgeRequest, Long userId) {
        ThrowUtil.businessError(problem == null, "题目不存在");
        ThrowUtil.businessError(userId == null, "用户身份无效");
        if (judgeRequest.getContestId() == null) {
            return;
        }
        ThrowUtil.businessError(judgeRequest.getContestId() <= 0, "比赛参数无效");
        boolean joined = contestService.isUserJoined(judgeRequest.getContestId(), userId);
        boolean isEnabled = contestService.isContestEnable(judgeRequest.getContestId());
        ThrowUtil.permissionDeny(!joined, "非法访问");
        ThrowUtil.businessError(!isEnabled, "不允许提交题目");
    }

    /**
     * 按题型校验提交参数，避免空代码、未知语言和空答案在业务深处才以 NPE 或脏判题记录暴露出来。
     */
    private void validateJudgeRequest(Problem problem, JudgeRequest judgeRequest) {
        ThrowUtil.businessError(judgeRequest.getProblemId() == null, "题目参数不能为空");
        ThrowUtil.businessError(problem == null, "题目不存在");
        if (problem.getType() == ProblemType.OJ) {
            ThrowUtil.businessError(judgeRequest.getLanguageId() == null, "请选择编程语言");
            ThrowUtil.businessError(!StringUtils.hasText(judgeRequest.getCode()), "请输入代码后再提交");
            ThrowUtil.businessError(judgeRequest.getCode().length() > 512 * 1024, "提交代码不能超过 512KB");
        } else {
            ThrowUtil.businessError(judgeRequest.getAnswers() == null, "答案参数不能为空");
        }
    }

    private void record(JudgeRequest judgeRequest, Long userId, ProblemJudgeResult judgeResult) {
        Long problemId = judgeRequest.getProblemId();
        Records records = new Records();

        UserAnswer userAnswer = new UserAnswer(judgeRequest.getAnswers(), judgeRequest.getCode(), judgeRequest.getLanguageId());
        records.setContestId(judgeRequest.getContestId());
        records.setProblemId(problemId);
        records.setUserId(userId);
        records.setScore(judgeResult.getTotalScore());
        records.setStatus(judgeResult.isCorrect());
        records.setAnswer(userAnswer);
        recordsService.addRecord(records);
    }

    @Override
    public ProblemJudgeResult judge(Long userId, JudgeRequest judgeRequest) {
        ThrowUtil.businessError(judgeRequest == null, "提交参数不能为空");
        ThrowUtil.businessError(judgeRequest.getProblemId() == null, "题目参数不能为空");
        Long problemId = judgeRequest.getProblemId();

        Problem problem = problemService.getById(problemId);
        validateJudgeRequest(problem, judgeRequest);
        beforeJudgeCheck(problem, judgeRequest, userId);


        ProblemJudgeResult judgeResult = judge(userId, problem, judgeRequest);

        // OJ题目不能在这里算分直接返回
        if (problem.getType() == ProblemType.OJ) {
            return judgeResult;
        }

        BigDecimal contestScore = null;
        if (judgeRequest.getContestId() != null) {
            contestScore = contestService.getScore(judgeRequest.getContestId(), problemId);
            ThrowUtil.businessError(contestScore == null, "题目不属于指定比赛");
        }

        // 添加做题记录
        record(judgeRequest, userId, judgeResult);

//        比赛题目不给答案 不显示对错 分数重算
        if (judgeRequest.getContestId() != null) {
            judgeResult.setAnswers(null);
            judgeResult.setCorrect(false);

//            比赛题目需要重新计算分数   (totalScore / fullMark) * score
            BigDecimal score = contestScore;

            BigDecimal fullMark = judgeResult.getFullMark();
            BigDecimal totalScore = judgeResult.getTotalScore();
            BigDecimal newScore = BigDecimal.ZERO;
            if (!fullMark.equals(BigDecimal.ZERO)) {
                 newScore = totalScore.divide(fullMark, RoundingMode.DOWN).multiply(score);
            }


//            新分数
            judgeResult.setTotalScore(newScore);
            judgeResult.setFullMark(score);

        }




        return judgeResult;
    }

    @Override
    public void codeTest(Long userId, TestRequest testRequest) {

        String language = sysLanguageService.getNameById(testRequest.getLanguageId());

        RunTestInfo runTestInfo = new RunTestInfo();
        runTestInfo.setUuid(testRequest.getUuid());
        runTestInfo.setUserId(userId);
        runTestInfo.setLanguage(language);
        runTestInfo.setCode(testRequest.getCode());
        runTestInfo.setStdin(testRequest.getStdin());
        redisJudgeTestUtil.save(new TestResult()
                .setUserId(userId)
                .setUuid(testRequest.getUuid())
                .setJudgeResult(JudgeResult.QUEUE), 120);

        try {
            Boolean isSuccess = judgeClient.test(runTestInfo).getData();
            if (!Boolean.TRUE.equals(isSuccess)) {
                redisJudgeTestUtil.save(new TestResult()
                        .setUserId(userId)
                        .setUuid(testRequest.getUuid())
                        .setJudgeResult(JudgeResult.JUDGE_ERROR), 120);
                ThrowUtil.businessError(true, "判题服务当前繁忙，请稍后重试");
            }
        } catch (RuntimeException e) {
            redisJudgeTestUtil.save(new TestResult()
                    .setUserId(userId)
                    .setUuid(testRequest.getUuid())
                    .setJudgeResult(JudgeResult.JUDGE_ERROR), 120);
            throw e;
        }
    }

    @Override
    public TestResult testStatus(Long userId, String uuid) {
        TestResult result = redisJudgeTestUtil.get(userId);
        return result != null && java.util.Objects.equals(result.getUuid(), uuid) ? result : null;
    }


}
