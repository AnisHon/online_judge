package com.anishan.problem.service.impl;

import com.anishan.commons.e.ProblemType;
import com.anishan.problem.domain.JudgeAnswer;
import com.anishan.problem.domain.dto.JudgeRequest;
import com.anishan.problem.domain.entity.ChoiceFillAnswers;
import com.anishan.problem.domain.entity.Problem;
import com.anishan.problem.domain.entity.Records;
import com.anishan.problem.domain.vo.ProblemJudgeResult;
import com.anishan.problem.service.*;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JudgeServiceImpl implements JudgeService {

    private final ProblemService problemService;
    private final OjProblemService ojProblemService;
    private final RecordsService recordsService;
    private final ChoiceFillAnswersService choiceFillAnswersService;

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

    }


    private ProblemJudgeResult doJudgeFill(List<ChoiceFillAnswers> answer, JudgeRequest judgeRequest) {
        Set<JudgeAnswer> answers = answer
                .stream()
                .map(a -> new JudgeAnswer(a.getBlankIndex(), a.getAnswerText()))
                .collect(Collectors.toSet());

        Set<JudgeAnswer> inputAnswers = judgeRequest
                .getAnswers()
                .stream()
                .peek(x -> x.setAnswer(x.getAnswer().strip()))
                .collect(Collectors.toSet());

        ProblemJudgeResult result = new ProblemJudgeResult();

        result.setRight(answerEquals(answers, inputAnswers));

        result.setAnswers(new ArrayList<>(answers));
        return result;
    }

    private ProblemJudgeResult judgeFill(Problem problem, JudgeRequest judgeRequest) {
        List<ChoiceFillAnswers> blankAnswers = choiceFillAnswersService.list(
                new LambdaUpdateWrapper<ChoiceFillAnswers>()
                        .eq(ChoiceFillAnswers::getProblemId, problem.getProblemId())
        );


        return doJudgeFill(blankAnswers, judgeRequest);
    }


    private ProblemJudgeResult doJudgeChoice(List<ChoiceFillAnswers> answer, JudgeRequest judgeRequest) {
        Set<JudgeAnswer> answers = answer
                .stream()
                .map(a -> new JudgeAnswer(a.getBlankIndex(), null)).collect(Collectors.toSet());


        Set<JudgeAnswer> inputAnswers = judgeRequest
                .getAnswers()
                .stream()
                .peek(x -> x.setAnswer(null))
                .collect(Collectors.toSet());
        ProblemJudgeResult result = new ProblemJudgeResult();

        result.setRight(answerEquals(answers, inputAnswers));

        result.setAnswers(new ArrayList<>(answers));
        return result;
    }

    private ProblemJudgeResult judgeChoice(Problem problem, JudgeRequest judgeRequest) {
        List<ChoiceFillAnswers> blankAnswers = choiceFillAnswersService.list(
                new LambdaUpdateWrapper<ChoiceFillAnswers>()
                        .eq(ChoiceFillAnswers::getProblemId, problem.getProblemId())
                        .eq(ChoiceFillAnswers::getIsCorrect, true)
        );

        return doJudgeChoice(blankAnswers, judgeRequest);


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


        Records records = new Records();


        records.setContestId(judgeRequest.getContestId());
        records.setAnswer(judgeRequest.getAnswers().toString());
        records.setProblemId(problemId);
        records.setUserId(userId);

        recordsService.addRecord(records);


        return judge;
    }



}
