package com.anishan.problem.mapper;

import com.anishan.problem.domain.entity.Problem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
* @author happy
* @description 针对表【problem(题目主表，OJ题目有分表，非OJ不需要继续分表)】的数据库操作Mapper
* @createDate 2024-10-16 22:39:16
* @Entity com.anishan.problem.entity.Problem
*/
public interface ProblemMapper extends BaseMapper<Problem> {

}




