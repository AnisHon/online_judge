package com.anishan.problem.service;

import com.anishan.problem.domain.entity.Problem;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author happy
* @description 针对表【problem(题目主表，OJ题目有分表，非OJ不需要继续分表)】的数据库操作Service
* @createDate 2024-10-16 22:39:16
*/
public interface ProblemService extends IService<Problem> {

}
