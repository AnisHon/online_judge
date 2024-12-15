package com.anishan.problem.service;

import com.anishan.problem.domain.entity.ProblemComplete;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author anishan
* @description 针对表【problem_complete(题目完成表)】的数据库操作Service
* @createDate 2024-12-14 00:43:42
*/
public interface ProblemCompleteService extends IService<ProblemComplete> {

    boolean exists(Long userId, Long problemId);

    boolean finish(Long userId, Long problemId);
}
