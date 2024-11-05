package com.anishan.problem.service;

import com.anishan.problem.domain.entity.OjProblemCase;
import com.anishan.api.client.problem.domain.vo.OjProblemCaseVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;
import java.util.List;

/**
* @author happy
* @description 针对表【oj_problem_case(OJ判题测试用例)】的数据库操作Service
* @createDate 2024-10-16 22:39:16
*/
public interface OjProblemCaseService extends IService<OjProblemCase> {

    LocalDateTime selectTime(Long id);

    List<OjProblemCaseVo> getByProblemId(Long problemId);
}
