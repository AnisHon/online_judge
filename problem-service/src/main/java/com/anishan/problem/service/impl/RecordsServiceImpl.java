package com.anishan.problem.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.anishan.api.util.NikeNameUtil;
import com.anishan.problem.domain.dto.UserAnswerRequest;
import com.anishan.problem.domain.entity.*;
import com.anishan.problem.domain.vo.*;
import com.anishan.problem.service.ContestService;
import com.anishan.problem.service.ProblemCompletionAwardService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.service.RecordsService;
import com.anishan.problem.mapper.RecordsMapper;
import com.anishan.problem.mapper.ContestRecordsMapper;
import com.anishan.problem.mapper.ContestAnswerRecordsMapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author happy
* @description 针对表【records(题目完成表)】的数据库操作Service实现
* @createDate 2024-10-16 22:39:16
*/
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RecordsServiceImpl extends ServiceImpl<RecordsMapper, Records>
    implements RecordsService{

    private final ContestService contestService;
    private final RecordsMapper recordsMapper;
    private final ContestRecordsMapper contestRecordsMapper;
    private final ContestAnswerRecordsMapper contestAnswerRecordsMapper;
    private final ProblemCompletionAwardService completionAwardService;

    /**
     * 判断是否存在，但是不返回boolean值
     * @param records 记录
     * @return 返回Long表示ID，不存在返回null
     */
    @Override
    public Long existRecord(Records records) {

        if (records == null || records.getProblemId() == null || records.getUserId() == null) {
            return null;
        }
        LambdaQueryWrapper<Records> wrapper = new LambdaQueryWrapper<Records>()
                .select(Records::getRecordId)
                .eq(records.getRecordId() != null, Records::getRecordId, records.getRecordId())
                .eq(Records::getProblemId, records.getProblemId())
                .eq(Records::getUserId, records.getUserId());
        wrapper = records.getContestId() == null
                ?
                wrapper.isNull(Records::getContestId)
                : wrapper.
                eq(Records::getContestId, records.getContestId());

        return this.getObj(wrapper, x -> (Long) x);
    }

    public void checkBeforeAdd(Records records) {
        if (records.getContestId() == null) {
            return;
        }
        boolean joined = contestService.isUserJoined(records.getContestId(), records.getUserId());
        if (!joined) {
            throw new RuntimeException("非法请求，用户未参加比赛(id: " + records.getContestId() + ")");
        }
    }
    
    /**
     * 添加一条记录，顺便会给用户加奖励分
     * @param records 记录类
     * @return 返回bool类型表示是否成功
     */
    @Transactional
    @Override
    public boolean addRecord(Records records) {
        if (records == null || records.getUserId() == null || records.getProblemId() == null) {
            return false;
        }
        BigDecimal score = records.getScore();
        if (score != null) {
            score = score.setScale(2, RoundingMode.HALF_DOWN);
            records.setScore(score);
        }

        if (records.getContestId() != null) {
            contestRecordsMapper.upsertLegacyGrade(IdWorker.getId(), records.getContestId(), records.getUserId(),
                    records.getProblemId(), records.getStatus(), records.getScore());
            ContestRecords projection = contestRecordsMapper.selectForUpdate(records.getContestId(),
                    records.getUserId(), records.getProblemId());
            if (projection == null || projection.getRecordId() == null) {
                throw new IllegalStateException("Contest score projection was not persisted");
            }
            if (records.getAnswer() != null) {
                contestAnswerRecordsMapper.upsertAnswer(projection.getRecordId(), records.getAnswer());
            }
            records.setRecordId(projection.getRecordId());
        } else {
            Long existingId = existRecord(records);
            records.setRecordId(existingId == null ? IdWorker.getId() : existingId);
            if (!this.saveOrUpdate(records)) {
                return false;
            }
        }

        if (Boolean.TRUE.equals(records.getStatus())) {
            completionAwardService.completeOnAc(records.getUserId(), records.getProblemId(), records.getScore());
        }
        return true;
    }

    @Override
    public BigDecimal score(Long contestId, Long userId) {
        return Optional.ofNullable(contestRecordsMapper.selectContestScore(contestId, userId))
                .orElse(BigDecimal.ZERO);
    }

    /**
     * 用户分数情况
     * todo Contest没有完善
     */
    @Override
    public List<UserStatistic> getUserStatistic(Long contestId) {

        List<UserStatistic> scores = recordsMapper.selectUserStatistic(contestId);
        NikeNameUtil.setNikeName(scores);
        scores.forEach(score -> {
            if (score.getScore() == null) {
                score.setScore(BigDecimal.ZERO);
            }
        });
        return scores;
    }

    /**
     * 题目正误情况
     */
    @Override
    public List<ProblemStatistic> getProblemStatistic(Long contestId) {

        long count = Db.count(Wrappers.lambdaQuery(UserContestRelation.class)
                .eq(UserContestRelation::getContestId, contestId));
        List<ProblemStatistic> problemStatistics = recordsMapper.selectProblemStatistic(contestId);

        problemStatistics.forEach(x -> {
            Integer rightNum = Optional.ofNullable(x.getRightNum()).orElse(0);
            Integer wrongNum = Optional.ofNullable(x.getWrongNum()).orElse(0);
            x.setAbsentNum((int) (count - rightNum - wrongNum));
        });

        return problemStatistics;
    }

    /**
     * 用户每道题的正误情况
     */
    @Override
    public List<UserScore> getUserScores(Long userId, Long contestId) {
        return recordsMapper.selectUserScore(userId, contestId);
    }

    @Override
    public List<ProblemScore> getProblemScores(Long problemId, Long contestId) {
        List<ProblemScore> problemScores = recordsMapper.selectProblemScore(problemId, contestId);
        NikeNameUtil.setNikeName(problemScores);
        return problemScores;
    }

    @Override
    public UserAnswer getAnswer(Long userId, UserAnswerRequest userAnswerRequest) {
        UserAnswer userAnswer;

        if (userAnswerRequest.getContestId() == null) {
            // 非比赛
            Records one = Db.getOne(Wrappers.lambdaQuery(Records.class)
                    .eq(Records::getUserId, userId)
                    .eq(Records::getProblemId, userAnswerRequest.getProblemId()));
            userAnswer = Optional.ofNullable(one).map(Records::getAnswer).orElse(null);
        } else  {
            // 比赛
            ContestRecords one = Db.getOne(Wrappers.lambdaQuery(ContestRecords.class)
                    .eq(ContestRecords::getContestId, userAnswerRequest.getContestId())
                    .eq(ContestRecords::getUserId, userId)
                    .eq(ContestRecords::getProblemId, userAnswerRequest.getProblemId()));

            Long recordId = Optional.ofNullable(one).map(ContestRecords::getRecordId).orElse(0L);
            ContestAnswerRecords contestAnswerRecords = Db.getById(recordId, ContestAnswerRecords.class);
            userAnswer = Optional.ofNullable(contestAnswerRecords).map(ContestAnswerRecords::getAnswer).orElse(null);
        }


        return userAnswer;
    }
}

