package com.anishan.problem.service;

import com.anishan.problem.domain.dto.OjProblemDto;
import com.anishan.problem.domain.entity.OjProblem;
import com.anishan.problem.domain.vo.OjProblemVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;

/**
* @author happy
* @description 针对表【oj_problem(OJ题目分表)】的数据库操作Service
* @createDate 2024-10-16 22:39:16
*/
public interface OjProblemService extends IService<OjProblem> {

    LocalDateTime getUpdateTime(Long id);

    boolean updateById(OjProblemDto ojProblem);

    OjProblemVo getOjProblemById(Long id);
}
