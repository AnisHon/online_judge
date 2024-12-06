package com.anishan.problem.service;

import com.anishan.problem.domain.entity.ChoiceFillAnswers;
import com.anishan.problem.domain.vo.ChoiceFillAnswersVo;
import com.anishan.problem.domain.vo.ProblemChoiceBlank;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author happy
* @description 针对表【choice_fill_answers(填空选择题答案表)】的数据库操作Service
* @createDate 2024-10-16 22:39:16
*/
public interface ChoiceFillAnswersService extends IService<ChoiceFillAnswers> {

    List<ProblemChoiceBlank> getChoice(Long problemId);

    List<ChoiceFillAnswersVo> getChoiceVo(Long problemId);

    List<ChoiceFillAnswersVo> getAnswerIndexes(Long problemId);
}
