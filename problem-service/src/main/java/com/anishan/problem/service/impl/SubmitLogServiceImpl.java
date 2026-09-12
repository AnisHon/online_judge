package com.anishan.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;

import com.anishan.api.client.problem.domain.dto.SubmitLogDto;
import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.api.client.judgeserver.domain.JudgeScore;
import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.commons.util.ThrowUtil;
import com.anishan.problem.domain.entity.SubmitLog;
import com.anishan.api.client.problem.domain.vo.SubmitLogVo;
import com.anishan.problem.mapper.SubmitLogMapper;
import com.anishan.problem.service.SubmitLogService;
import com.anishan.problem.util.SubmitLogUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
* @author happy
* @description 针对表【submit_log(OJ判题提交记录)】的数据库操作Service实现
* @createDate 2024-10-16 22:39:16
*/
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SubmitLogServiceImpl extends ServiceImpl<SubmitLogMapper, SubmitLog>
    implements SubmitLogService {

    private final SubmitLogUtil submitLogUtil;

    @Override
    public Long logQueue(Long userId, Long problemId, String language) {
        SubmitLog submitLog = new SubmitLog()
                .setLanguage(language)
                .setStatus(JudgeResult.QUEUE)
                .setUserId(userId)
                .setProblemId(problemId);

        this.save(submitLog);
        submitLogUtil.cacheLog(submitLog);
        return submitLog.getSubmitId();
    }

    @Override
    public Long createQueued(JudgeInfo judgeInfo) {
        ThrowUtil.runtime(judgeInfo == null, "判题参数不能为空");
        SubmitLog submitLog = new SubmitLog()
                .setUserId(judgeInfo.getUserId())
                .setProblemId(judgeInfo.getProblemId())
                .setContestId(judgeInfo.getContestId())
                .setLanguage(judgeInfo.getLanguage())
                .setCode(judgeInfo.getCode())
                .setStatus(JudgeResult.QUEUE);
        this.save(submitLog);
        submitLogUtil.cacheLog(submitLog);
        return submitLog.getSubmitId();
    }

    @Override
    public Long logJudge(SubmitLogDto log) {
        SubmitLog submitLog = BeanUtil.copyProperties(log, SubmitLog.class);
        this.save(submitLog);
        return submitLog.getSubmitId();
    }


    @Override
    public boolean changeStatus(Long userId, Long id, JudgeResult result) {

        submitLogUtil.update(new SubmitLog()
                .setUserId(userId)
                .setStatus(result)
        );
        return this.update(
                new LambdaUpdateWrapper<SubmitLog>()
                        .set(SubmitLog::getStatus, result)
                        .eq(SubmitLog::getSubmitId, id)
                        .eq(SubmitLog::getUserId, userId)
        );
    }

    @Override
    public SubmitLogVo getLog(Long id, Long userId) {
        if (id == null) {
            return null;
        }

        // Redis 只缓存每个用户的最后一条提交，不能拿它回答指定 ID 的轮询请求。
        SubmitLog submitLog = this.getById(id);
        if (submitLog == null || !userId.equals(submitLog.getUserId())) {
            return null;
        }
        return BeanUtil.copyProperties(submitLog, SubmitLogVo.class);
    }

    @Override
    public boolean update(SubmitLogDto submitLog) {
        if (submitLog == null) {
            return false;
        }

        SubmitLog log = BeanUtil.copyProperties(submitLog, SubmitLog.class);

        // 缓存也改一下

        submitLogUtil.update(log);
        return this.updateById(log);
    }

    @Override
    public boolean updateStatus(JudgeScore judgeScore) {
        if (judgeScore == null || judgeScore.getSubmitId() == null || judgeScore.getResult() == null) {
            return false;
        }
        SubmitLog current = this.getById(judgeScore.getSubmitId());
        if (current == null
                || (judgeScore.getUserId() != null && !judgeScore.getUserId().equals(current.getUserId()))
                || !canTransition(current.getStatus(), judgeScore.getResult())) {
            return false;
        }
        boolean updated = this.update(new LambdaUpdateWrapper<SubmitLog>()
                .set(SubmitLog::getStatus, judgeScore.getResult())
                .eq(SubmitLog::getSubmitId, judgeScore.getSubmitId())
                .eq(judgeScore.getUserId() != null, SubmitLog::getUserId, judgeScore.getUserId()));
        if (updated && judgeScore.getUserId() != null) {
            submitLogUtil.update(new SubmitLog()
                    .setUserId(judgeScore.getUserId())
                    .setSubmitId(judgeScore.getSubmitId())
                    .setStatus(judgeScore.getResult()));
        }
        return updated;
    }

    private boolean canTransition(JudgeResult current, JudgeResult next) {
        if (current == null || current == next) {
            return true;
        }
        if (current != JudgeResult.QUEUE && current != JudgeResult.COMPILING && current != JudgeResult.RUNNING) {
            return false;
        }
        return current == JudgeResult.QUEUE
                ? next == JudgeResult.COMPILING || next == JudgeResult.JUDGE_ERROR
                : current == JudgeResult.COMPILING
                ? next == JudgeResult.RUNNING || next == JudgeResult.COMPILE_ERROR || next == JudgeResult.JUDGE_ERROR
                : next != JudgeResult.QUEUE && next != JudgeResult.COMPILING && next != JudgeResult.RUNNING;
    }

    @Override
    public boolean complete(JudgeScore judgeScore) {
        if (judgeScore == null || judgeScore.getSubmitId() == null) {
            return false;
        }
        JudgeResult result = judgeScore.getResult() == null ? JudgeResult.JUDGE_ERROR : judgeScore.getResult();
        SubmitLog log = new SubmitLog()
                .setSubmitId(judgeScore.getSubmitId())
                .setUserId(judgeScore.getUserId())
                .setProblemId(judgeScore.getProblemId())
                .setContestId(judgeScore.getContestId())
                .setCode(judgeScore.getCode())
                .setStatus(result)
                .setTime(judgeScore.getRuntime())
                .setMemory(judgeScore.getMemory())
                .setTotalCount(judgeScore.getTotalCount())
                .setPassCount(judgeScore.getPassCount())
                .setInternalError(judgeScore.getInternalError())
                .setErrorCode(judgeScore.getErrorCode())
                // 判题基础设施异常详情不进入用户可见 stderr。
                .setStderr(result == JudgeResult.JUDGE_ERROR ? null : judgeScore.getErrorMessage());
        boolean updated = this.updateById(log);
        if (updated && judgeScore.getUserId() != null) {
            submitLogUtil.update(log);
        }
        return updated;
    }


}

