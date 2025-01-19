package com.anishan.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;

import com.anishan.api.client.problem.domain.dto.SubmitLogDto;
import com.anishan.commons.enumeration.JudgeResult;
import com.anishan.commons.util.ThrowUtil;
import com.anishan.problem.config.JudgeConfig;
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
    private final JudgeConfig judgeConfig;

    @Override
    public Long logQueue(Long userId, Long problemId, String language) {
        SubmitLog submitLog = new SubmitLog()
                .setLanguage(language)
                .setStatus(JudgeResult.QUEUE)
                .setUserId(userId)
                .setProblemId(problemId);

        ThrowUtil.runtime(submitLogUtil.isExist(userId), "请等待");


        submitLogUtil.cacheLog(submitLog);

        this.save(submitLog);
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
        submitLogUtil.setExpired(userId, 20L);


        return this.update(
                new LambdaUpdateWrapper<SubmitLog>()
                        .set(SubmitLog::getStatus, result)
                        .eq(SubmitLog::getSubmitId, id)
        );
    }

    @Override
    public SubmitLogVo getLog(Long id, Long userId) {
        if (id == null) {
            return null;
        }

        SubmitLog submitLog = submitLogUtil.get(userId);



        if (submitLog == null) {
            submitLog = this.getById(id);
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
        submitLogUtil.setExpired(submitLog.getUserId(), judgeConfig.getJudgeInterval().longValue());

        return this.updateById(log);
    }


}




