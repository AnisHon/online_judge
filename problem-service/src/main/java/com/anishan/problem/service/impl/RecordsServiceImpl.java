package com.anishan.problem.service.impl;

import com.anishan.api.client.user.client.UserClient;
import com.anishan.api.client.user.client.UserInternalClient;
import com.anishan.api.client.user.domain.vo.UserVo;
import com.anishan.problem.config.JudgeConfig;
import com.anishan.problem.domain.vo.ScoredUser;
import com.anishan.problem.domain.vo.ProblemStatistic;
import com.anishan.problem.service.ContestService;
import com.anishan.problem.service.ProblemCompleteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.domain.entity.Records;
import com.anishan.problem.service.RecordsService;
import com.anishan.problem.mapper.RecordsMapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final UserClient userClient;
    private final JudgeConfig judgeConfig;
    private final UserInternalClient userInternalClient;
    private final ProblemCompleteService problemCompleteService;

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
    
    private void doAddPoint(Records newRec, Records oldRec) {
        boolean newStatus = newRec.isStatus();
        boolean complete = problemCompleteService.exists(newRec.getUserId(), newRec.getProblemId());
        // 新纪录是正确的，旧记录没有或者不对，才加分
        boolean isScoreAddable = !complete && newStatus && (oldRec == null || !oldRec.isStatus());

        if (!isScoreAddable) {
            return;
        }

        BigDecimal point = judgeConfig.getIsFixedAwardPoint() ? judgeConfig.getAwardPoint() : newRec.getScore();

        userInternalClient.addPoint(point.toString(), newRec.getUserId());

    }


    private Long addPoint(Records records) {
        if (records == null || records.getProblemId() == null || records.getUserId() == null) {
            return null;
        }
        LambdaQueryWrapper<Records> wrapper = new LambdaQueryWrapper<Records>()
                .select(Records::getRecordId, Records::isStatus)
                .eq(records.getRecordId() != null, Records::getRecordId, records.getRecordId())
                .eq(Records::getProblemId, records.getProblemId())
                .eq(Records::getUserId, records.getUserId());
        wrapper = records.getContestId() == null ?
                wrapper.isNull(Records::getContestId)
                :
                wrapper.eq(Records::getContestId, records.getContestId());

        Records one = this.getOne(wrapper);


        doAddPoint(records, one);

        return Optional.ofNullable(one).map(Records::getRecordId).orElse(null);
    }

    /**
     * 添加一条记录，顺便会给用户加奖励分
     * @param records 记录类
     * @return 返回bool类型表示是否成功
     */
    @Transactional
    @Override
    public boolean addRecord(Records records) {
        Long recordId = addPoint(records);
        boolean b;
        if (recordId == null) {
            b = this.save(records);
        } else {
            records.setRecordId(recordId);
            b = this.updateById(records);
        }

        // 标记已完成
        problemCompleteService.finish(records.getUserId(), records.getProblemId());

        return b;
    }

    @Override
    public BigDecimal score(Long contestId, Long userId) {
        MPJLambdaWrapper<Records> wrapper = new MPJLambdaWrapper<Records>()
                .selectSum(Records::getScore)
                .eq(Records::getContestId, contestId)
                .eq(Records::getUserId, userId);

        List<BigDecimal> score = recordsMapper.selectObjs(wrapper);

        return score.stream().findFirst().orElse(null);
    }

    @Override
    public List<ScoredUser> rank(Long contestId) {
        MPJLambdaWrapper<Records> wrapper = new MPJLambdaWrapper<Records>()
                .select("user_id")
                .selectSum(Records::getScore, "score")
                .eq(Records::getContestId, contestId)
                .groupBy(Records::getUserId);

        List<Map<String, Object>> maps = recordsMapper.selectJoinMaps(wrapper);

        List<Long> ids = maps.stream().map(x -> (Long) x.get("user_id")).collect(Collectors.toList());

        HashMap<Long, UserVo> users = new HashMap<>();

        userClient.listUser(ids).getData().forEach(x -> users.put(x.getUserId(), x));


        List<ScoredUser> scoredUsers = new ArrayList<>();


        maps.forEach(x -> {
            Long id = (Long) x.get("user_id");
            ScoredUser score = new ScoredUser(users.get(id), (BigDecimal) x.get("score"));
            scoredUsers.add(score);
        });



        return scoredUsers;
    }

    @Override
    public List<ProblemStatistic> statistic(Long contestId) {
        return recordsMapper.statistic(contestId);
    }
}




