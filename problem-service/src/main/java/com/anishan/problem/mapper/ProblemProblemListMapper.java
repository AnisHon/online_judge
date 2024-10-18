package com.anishan.problem.mapper;

import com.anishan.problem.domain.entity.ProblemListRelation;
import com.anishan.problem.domain.entity.ProblemProblemListRelation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.lettuce.core.dynamic.annotation.Param;

import java.util.List;

/**
* @author happy
* @description 针对表【problem_problem_list(题单 题目关系表)】的数据库操作Mapper
* @createDate 2024-10-16 22:40:59
* @Entity com.anishan.problem.entity.ProblemProblemListRelation
*/
public interface ProblemProblemListMapper extends BaseMapper<ProblemProblemListRelation> {
    List<ProblemListRelation> getByListId(Long listId);
}




