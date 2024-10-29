package com.anishan.problem.service.impl;

import com.anishan.commons.e.ProblemType;
import com.anishan.commons.util.ThrowUtil;
import com.anishan.problem.domain.JudgeAnswer;
import com.anishan.problem.domain.ScoreAndIsCorrected;
import com.anishan.problem.domain.dto.JudgeRequest;
import com.anishan.problem.domain.entity.ChoiceFillAnswers;
import com.anishan.problem.domain.entity.Problem;
import com.anishan.problem.domain.entity.Records;
import com.anishan.problem.domain.entity.SysLanguage;
import com.anishan.problem.domain.vo.ProblemJudgeResult;
import com.anishan.problem.service.*;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JudgeServiceImpl implements JudgeService {

    private final ProblemService problemService;
    private final OjProblemService ojProblemService;
    private final ProblemListService problemListService;
    private final RecordsService recordsService;
    private final ChoiceFillAnswersService choiceFillAnswersService;
    private final SysLanguageService sysLanguageService;

    public static boolean answerEquals(Set<?> set1, Set<?> set2){
        if(set1 == null || set2 ==null){
            return false;
        }
        if(set1.size()!=set2.size()){
            return false;
        }
        return set1.containsAll(set2);
    }


    private void judgeOj(Problem problem, JudgeRequest judgeRequest) {
        SysLanguage language = sysLanguageService.getById(judgeRequest.getLanguageId());
        ThrowUtil.runtime(language == null, "不支持的语言");



        //todo
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
        if (!blankAnswers.containsKey(index)) {
            return ScoreAndIsCorrected.wrong();
        }

        String userAnswers = judgeAnswer.getAnswer().trim();

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
     * 判断是否正确，正确给分，否则0分，用于填空题
     * @param judgeAnswer 用户输入的答案
     * @param choiceFillAnswers 标准答案
     * @return 分数和是否正确
     */
    private ScoreAndIsCorrected judgeScore(
            JudgeAnswer judgeAnswer,
            List<ChoiceFillAnswers> choiceFillAnswers
    ) {
        Integer index = judgeAnswer.getIndex();
        ScoreAndIsCorrected scoreAndIsCorrected = new ScoreAndIsCorrected();
        scoreAndIsCorrected.setScore(BigDecimal.ZERO);
        scoreAndIsCorrected.setCorrected(false);
        for (ChoiceFillAnswers choiceFillAnswer : choiceFillAnswers) {
            if (Objects.equals(choiceFillAnswer.getBlankIndex(), index)) {
                scoreAndIsCorrected.setCorrected(true);
                scoreAndIsCorrected.setScore(choiceFillAnswer.getScore());
            }
        }
        return  scoreAndIsCorrected;
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
        List<ChoiceFillAnswers> blankAnswers = choiceFillAnswersService.list(
                new LambdaUpdateWrapper<ChoiceFillAnswers>()
                        .eq(ChoiceFillAnswers::getProblemId, problem.getProblemId())
        );


        ProblemJudgeResult judgeResult = doJudgeFill(blankAnswers, judgeRequest);

        judgeResult.setAnswers(blankAnswers.stream().map(x -> new JudgeAnswer(x.getBlankIndex(), x.getAnswerText())).collect(Collectors.toList()));

        return judgeResult;
    }


    private ProblemJudgeResult doJudgeChoice(List<ChoiceFillAnswers> answer, JudgeRequest judgeRequest) {

        ProblemJudgeResult problemJudgeResult = new ProblemJudgeResult();
        problemJudgeResult.setTotalScore(BigDecimal.ZERO);
        problemJudgeResult.setCorrect(false);

        judgeRequest.getAnswers().forEach(x -> x.setAnswer(null));
        List<JudgeAnswer> collect = judgeRequest
                .getAnswers()
                .stream()
                .distinct()
                .collect(Collectors.toList());

        if (answer.size() < judgeRequest.getAnswers().size()) {
            return problemJudgeResult;
        }

        for (JudgeAnswer judgeRequestAnswer : collect) {
            ScoreAndIsCorrected scoreAndIsCorrected = judgeScore(judgeRequestAnswer, answer);
            if (scoreAndIsCorrected.isCorrected()) {
                problemJudgeResult.add(scoreAndIsCorrected.getScore());
            } else {
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
        return problemJudgeResult;
    }



    private ProblemJudgeResult judge(Problem problem, JudgeRequest judgeRequest) {
        ProblemType type = problem.getType();
        ProblemJudgeResult judgeResult = null;
        switch (type) {
            case OJ:
                judgeOj(problem, judgeRequest);
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

    @Override
    public ProblemJudgeResult judge(Long userId, JudgeRequest judgeRequest) {
        Long problemId = judgeRequest.getProblemId();

        Problem problem = problemService.getById(problemId);

        ProblemJudgeResult judge = judge(problem, judgeRequest);

        if (judgeRequest.getContestId() != null) {
            judge.setAnswers(null);
        }


        // 添加做题记录

        ObjectMapper json = new ObjectMapper();

        Records records = new Records();

        records.setContestId(judgeRequest.getContestId());
        records.setProblemId(problemId);
        records.setUserId(userId);
        records.setScore(judge.getTotalScore());
        records.setStatus(judge.isCorrect() ? 1 : 0);
        try {
            records.setAnswer(json.writeValueAsString(judgeRequest.getAnswers()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        recordsService.addRecord(records);


        return judge;
    }



}
