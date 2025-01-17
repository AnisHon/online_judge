package com.anishan.problem.service;

import com.anishan.problem.domain.dto.UserAnswerRequest;
import com.anishan.problem.domain.entity.Records;
import com.anishan.problem.domain.vo.*;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

/**
* @author happy
* @description 针对表【records(题目完成表)】的数据库操作Service
* @createDate 2024-10-16 22:39:16
*/
public interface RecordsService extends IService<Records> {


    Long existRecord(Records records);

    boolean addRecord(Records records);

    BigDecimal score(Long contestId, Long userId);

    List<UserStatistic> getUserStatistic(Long contestId);

    List<ProblemStatistic> getProblemStatistic(Long contestId);

    List<UserScore> getUserScores(Long userId, Long contestId);

    List<ProblemScore> getProblemScores(Long problemId, Long contestId);

    UserAnswer getAnswer(Long userId, UserAnswerRequest userAnswerRequest);
}
