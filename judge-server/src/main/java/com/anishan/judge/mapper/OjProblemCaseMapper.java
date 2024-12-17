package com.anishan.judge.mapper;

import com.anishan.api.domain.entity.OjProblemCase;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author happy
* @description 针对表【oj_problem_case(OJ判题测试用例)】的数据库操作Mapper
* @createDate 2024-10-16 22:39:16
* @Entity com.anishan.problem.entity.OjProblemCase
*/
@Mapper
public interface OjProblemCaseMapper extends BaseMapper<OjProblemCase> {

}




