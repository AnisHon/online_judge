package com.anishan.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.anishan.api.client.problem.domain.vo.OjProblemCaseVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.domain.entity.OjProblemCase;
import com.anishan.problem.service.OjProblemCaseService;
import com.anishan.problem.mapper.OjProblemCaseMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
* @author happy
* @description 针对表【oj_problem_case(OJ判题测试用例)】的数据库操作Service实现
* @createDate 2024-10-16 22:39:16
*/
@Service
public class OjProblemCaseServiceImpl extends ServiceImpl<OjProblemCaseMapper, OjProblemCase>
    implements OjProblemCaseService{

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

}




