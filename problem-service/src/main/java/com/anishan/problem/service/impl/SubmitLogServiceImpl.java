package com.anishan.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.anishan.api.client.judgeserver.domain.JudgeInfo;
import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.commons.util.ThrowUtil;
import com.anishan.problem.domain.entity.SubmitLog;
import com.anishan.api.client.problem.domain.vo.SubmitLogVo;
import com.anishan.problem.mapper.SubmitLogMapper;
import com.anishan.problem.service.SubmitLogService;
import com.anishan.problem.util.SubmitLogUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
* @author happy
* @description 针对表【submit_log(OJ判题提交记录)】的数据库操作Service实现
* @createDate 2024-10-16 22:39:16
*/
@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SubmitLogServiceImpl extends ServiceImpl<SubmitLogMapper, SubmitLog>
    implements SubmitLogService {

    private final SubmitLogUtil submitLogUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
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
        cacheAfterCommit(submitLog);
        return submitLog.getSubmitId();
    }

    private void cacheAfterCommit(SubmitLog submitLog) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            cacheSafely(submitLog);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cacheSafely(submitLog);
            }
        });
    }

    private void cacheSafely(SubmitLog submitLog) {
        try {
            submitLogUtil.cacheLog(submitLog);
        } catch (RuntimeException cacheFailure) {
            // The database/outbox commit is authoritative; cache failure must not turn it into a fake rejection.
            log.warn("提交日志缓存写入失败 submitId={} errorType={}",
                    submitLog.getSubmitId(), cacheFailure.getClass().getSimpleName());
        }
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

}
