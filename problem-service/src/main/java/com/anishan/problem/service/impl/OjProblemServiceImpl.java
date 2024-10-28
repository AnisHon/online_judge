package com.anishan.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.anishan.problem.domain.dto.OjProblemDto;
import com.anishan.problem.domain.vo.OjProblemVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.domain.entity.OjProblem;
import com.anishan.problem.service.OjProblemService;
import com.anishan.problem.mapper.OjProblemMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
* @author happy
* @description 针对表【oj_problem(OJ题目分表)】的数据库操作Service实现
* @createDate 2024-10-16 22:39:16
*/
@Service
public class OjProblemServiceImpl extends ServiceImpl<OjProblemMapper, OjProblem>
    implements OjProblemService{


    @Override
    public LocalDateTime getUpdateTime(Long id) {
        return this.getObj(
                new LambdaQueryWrapper<OjProblem>()
                        .select(OjProblem::getUpdateTime)
                        .eq(OjProblem::getProblemId, id),
                x -> (LocalDateTime) x
        );
    }

    @Override
    public boolean updateById(OjProblemDto ojProblem) {
        OjProblem entity = BeanUtil.copyProperties(ojProblem, OjProblem.class);
        entity.setUpdateTime(getUpdateTime(ojProblem.getProblemId()));
        return this.updateById(entity);
    }

    @Override
    public OjProblemVo getOjProblemById(Long id) {
        OjProblem problem = this.getById(id);
        return BeanUtil.copyProperties(problem, OjProblemVo.class);
    }
}




