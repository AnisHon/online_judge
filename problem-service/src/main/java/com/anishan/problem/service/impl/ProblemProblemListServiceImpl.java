package com.anishan.problem.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.anishan.problem.domain.dto.ProblemListRelationDto;
import com.anishan.problem.domain.entity.ProblemListRelation;
import com.anishan.problem.domain.vo.ProblemListRelationVo;
import com.anishan.problem.service.ProblemService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.domain.entity.ProblemProblemListRelation;
import com.anishan.problem.service.ProblemProblemListService;
import com.anishan.problem.mapper.ProblemProblemListMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
* @author happy
* @description 针对表【problem_problem_list(题单 题目关系表)】的数据库操作Service实现
* @createDate 2024-10-16 22:40:59
*/
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ProblemProblemListServiceImpl extends ServiceImpl<ProblemProblemListMapper, ProblemProblemListRelation>
    implements ProblemProblemListService{

    private final ProblemProblemListMapper problemProblemListMapper;

    @Override
    public List<Long> getProblemIds(Long id) {
        return this.listObjs(
                new LambdaQueryWrapper<ProblemProblemListRelation>()
                        .select(ProblemProblemListRelation::getProblemId)
                        .eq(ProblemProblemListRelation::getListId, id),
                pId -> (Long) pId
        );
    }

    @Override
    public List<ProblemListRelationVo> getListedProblem(Long listId) {
        List<ProblemListRelation> ppr = problemProblemListMapper.getByListId(listId);
        return ppr.stream().map(ProblemListRelation::toVo).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delByListIds(List<Long> ids) {
        if (CollectionUtil.isEmpty(ids)) {
            return;
        }
        this.remove(
                new LambdaUpdateWrapper<>()
        );
    }






}





