package com.anishan.problem.mapper;

import com.anishan.problem.domain.entity.ChoiceFillAnswers;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author happy
* @description 针对表【choice_fill_answers(填空选择题答案表)】的数据库操作Mapper
* @createDate 2024-10-16 22:39:16
* @Entity com.anishan.problem.entity.ChoiceFillAnswers
*/
public interface ChoiceFillAnswersMapper extends BaseMapper<ChoiceFillAnswers> {

    List<ChoiceFillAnswers> selectByProblemIdForUpdate(@Param("problemId") Long problemId);

}



