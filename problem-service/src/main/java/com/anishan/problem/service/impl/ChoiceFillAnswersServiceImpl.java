package com.anishan.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.anishan.problem.domain.vo.ChoiceFillAnswersVo;
import com.anishan.problem.domain.vo.ProblemChoiceBlank;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.domain.entity.ChoiceFillAnswers;
import com.anishan.problem.service.ChoiceFillAnswersService;
import com.anishan.problem.mapper.ChoiceFillAnswersMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
* @author happy
* @description 针对表【choice_fill_answers(填空选择题答案表)】的数据库操作Service实现
* @createDate 2024-10-16 22:39:16
*/
@Service
public class ChoiceFillAnswersServiceImpl extends ServiceImpl<ChoiceFillAnswersMapper, ChoiceFillAnswers>
    implements ChoiceFillAnswersService{

    @Override
    public List<ProblemChoiceBlank> getChoice(Long problemId) {
       return this.list(new LambdaQueryWrapper<ChoiceFillAnswers>()
               .eq(ChoiceFillAnswers::getProblemId, problemId)
               .orderByAsc(ChoiceFillAnswers::getBlankIndex)
        ).stream().map(x -> new ProblemChoiceBlank(null, x.indexToChoice(), x.getAnswerText())).collect(Collectors.toList());
    }

    @Override
    public List<ChoiceFillAnswersVo> getChoiceVo(Long problemId) {
        List<ChoiceFillAnswers> list = this.list(new LambdaQueryWrapper<ChoiceFillAnswers>()
                .eq(ChoiceFillAnswers::getProblemId, problemId)
                .orderByAsc(ChoiceFillAnswers::getBlankIndex)
        );
        return BeanUtil.copyToList(list, ChoiceFillAnswersVo.class);
    }

    @Override
    public List<ChoiceFillAnswersVo> getAnswerIndexes(Long problemId) {
        List<ChoiceFillAnswers> list = this.list(new QueryWrapper<ChoiceFillAnswers>()
                .select("distinct blank_index")
                .lambda()
                .eq(ChoiceFillAnswers::getProblemId, problemId)
        );
        return BeanUtil.copyToList(list, ChoiceFillAnswersVo.class);

    }


    @Override
    @Cacheable(cacheNames = "problem:choice-fill:", key = "#problemId")
    public List<ChoiceFillAnswers> get(Long problemId) {
        this.list(
                new LambdaUpdateWrapper<ChoiceFillAnswers>()
                        .eq(ChoiceFillAnswers::getProblemId,problemId)
        );
        return List.of();
    }
}




