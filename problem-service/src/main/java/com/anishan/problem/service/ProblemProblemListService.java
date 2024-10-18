package com.anishan.problem.service;

import com.anishan.problem.domain.dto.ProblemListRelationDto;
import com.anishan.problem.domain.entity.ProblemProblemListRelation;
import com.anishan.problem.domain.vo.ProblemListRelationVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author happy
* @description 针对表【problem_problem_list(题单 题目关系表)】的数据库操作Service
* @createDate 2024-10-16 22:40:59
*/
public interface ProblemProblemListService extends IService<ProblemProblemListRelation> {

    List<Long> getProblemIds(Long id);

    List<ProblemListRelationVo> getListedProblem(Long listId);

    void delByListIds(List<Long> ids);

}
