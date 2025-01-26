package com.anishan.problem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.domain.entity.ProblemComplete;
import com.anishan.problem.service.ProblemCompleteService;
import com.anishan.problem.mapper.ProblemCompleteMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
* @author anishan
* @description 针对表【problem_complete(题目完成表)】的数据库操作Service实现
* @createDate 2024-12-14 00:43:42
*/
@Service
public class ProblemCompleteServiceImpl extends ServiceImpl<ProblemCompleteMapper, ProblemComplete>
    implements ProblemCompleteService{

    @Override
    public boolean exists(Long userId, Long problemId) {
        return this.exists(
                new LambdaQueryWrapper<ProblemComplete>()
                        .eq(ProblemComplete::getUserId, userId)
                        .eq(ProblemComplete::getProblemId, problemId)
        );
    }

    @Override
    @Transactional
    public boolean finish(Long userId, Long problemId) {
        ProblemComplete problemComplete = new ProblemComplete();
        problemComplete.setUserId(userId);
        problemComplete.setProblemId(problemId);


        // 忽略主键冲突异常
        boolean save = true;
        if (!exists(userId, problemId)) {
            save = this.save(problemComplete);
        }

        return save;
    }
}




