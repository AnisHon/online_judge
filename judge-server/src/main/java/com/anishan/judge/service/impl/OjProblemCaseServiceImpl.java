package com.anishan.judge.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.anishan.api.client.judgeserver.domain.OjProblemCaseDto;
import com.anishan.api.client.problem.domain.vo.OjProblemCaseVo;
import com.anishan.api.domain.entity.OjProblemCase;
import com.anishan.judge.judge.JudgeCase;
import com.anishan.judge.mapper.OjProblemCaseMapper;
import com.anishan.judge.service.JudgeService;
import com.anishan.judge.service.OjProblemCaseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.BeanUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
* @author happy
* @description 针对表【oj_problem_case(OJ判题测试用例)】的数据库操作Service实现
* @createDate 2024-10-16 22:39:16
*/
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OjProblemCaseServiceImpl extends ServiceImpl<OjProblemCaseMapper, OjProblemCase>
    implements OjProblemCaseService {

    private final JudgeCase judgeCase;

    @Override
    public LocalDateTime selectTime(Long id) {
        return this.getObj(new LambdaQueryWrapper<OjProblemCase>()
                .select(OjProblemCase::getUpdateTime)
                .eq(OjProblemCase::getCaseId, id),
                x -> (LocalDateTime) x
        );
    }

    @Override
    public List<OjProblemCaseVo> getByProblemId(Long problemId) {
        List<OjProblemCase> list = list(new LambdaQueryWrapper<OjProblemCase>()
                .eq(OjProblemCase::getProblemId, problemId)
        );


        return BeanUtil.copyToList(list, OjProblemCaseVo.class);
    }

    @Override
    @Transactional
    public void setCases(Long problemId, List<OjProblemCase> cases) {
//        this.saveOrUpdateBatch(cases);
        for (OjProblemCase case_ : cases) {
            judgeCase.setCase(problemId, case_.getCaseId(), case_.getInput(), case_.getOutput());
        }
    }


}




