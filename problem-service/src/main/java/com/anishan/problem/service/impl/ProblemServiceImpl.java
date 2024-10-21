package com.anishan.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.anishan.commons.e.ProblemAuth;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.problem.domain.dto.PagedProblem;
import com.anishan.problem.domain.vo.*;
import com.anishan.problem.service.ChoiceFillAnswersService;
import com.anishan.problem.service.OjProblemService;
import com.anishan.problem.service.TagService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.domain.entity.Problem;
import com.anishan.problem.service.ProblemService;
import com.anishan.problem.mapper.ProblemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author happy
* @description 针对表【problem(题目主表，OJ题目有分表，非OJ不需要继续分表)】的数据库操作Service实现
* @createDate 2024-10-16 22:39:16
*/
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ProblemServiceImpl extends ServiceImpl<ProblemMapper, Problem>
    implements ProblemService{

    private final OjProblemService ojProblemService;
    private final ProblemMapper problemMapper;
    private final ChoiceFillAnswersService choiceFillAnswersService;
    private final TagService tagService;

    public Problem doGetProblem(Long id) {
        return this.getOne(new LambdaQueryWrapper<Problem>()
                .eq(Problem::getProblemId, id)
                .ne(Problem::getAuth, ProblemAuth.Contest)
        );
    }

    public ProblemVo toVo(Problem problem) {
        return BeanUtil.copyProperties(problem, ProblemVo.class);
    }

    @Override
    public ProblemVo getProblemById(Long id) {

        Problem problem = doGetProblem(id);

        return toVo(problem);
    }

    public List<Problem> doGetProblems(PagedProblem pagedProblem) {
        Page<Problem> page = pagedProblem.page();
        return problemMapper
                .selectAllByProblemIdAndTagId(page, pagedProblem.getProblemId(), pagedProblem.getTagIds());
    }

    @Override
    public PagedResult<ProblemVo> getProblems(PagedProblem pagedProblem) {
        List<Problem> problems = doGetProblems(pagedProblem);
        List<ProblemVo> problemVos = BeanUtil.copyToList(problems, ProblemVo.class);

        Long l = problemMapper.selectAllCountByProblemIdAndTagId(
                pagedProblem.getProblemId(),
                pagedProblem.getTagIds()
        );

        return PagedResult.fromPage(pagedProblem.page(), problemVos, l);
    }


    @Override
    public PagedResult<TaggedProblemVo> listTaggerProblems(PagedProblem pagedProblem) {
        Page<Problem> page = pagedProblem.page();
        List<TaggedProblemVo> taggedProblemVos = problemMapper
                .selectTaggedProblemByProblemIdAndTagId(page, pagedProblem.getProblemId(), pagedProblem.getTagIds());
        Long l = problemMapper.selectAllCountByProblemIdAndTagId(
                pagedProblem.getProblemId(),
                pagedProblem.getTagIds()
        );

        return PagedResult.fromPage(page, taggedProblemVos, l);
    }


    public DetailProblem doGetDetail(ProblemVo problem, Long ojId) {
        if (problem == null) {
            return null;
        }

        List<TagVo> tags = tagService.getTagByProblemId(problem.getProblemId());

        DetailProblem detailProblem = new DetailProblem(problem, null, null, tags);
        switch (problem.getType()) {
            case OJ:
                OjProblemVo ojProblem = ojProblemService.getOjProblemById(ojId);
                detailProblem.setOjProblemVo(ojProblem);
                break;
            case FILL:
                break;
            case CHOICE:
                List<ProblemChoice> choices = choiceFillAnswersService.getChoice(problem.getProblemId());
                detailProblem.setChoices(choices);
                break;
        }

        return detailProblem;
    }

    @Override
    public DetailProblem getDetailProblem(Long id) {
        Problem problem = doGetProblem(id);
        ProblemVo problemVo = toVo(problem);

        return doGetDetail(problemVo, problem.getOjId());
    }



    @Override
    public List<ProblemVo> getBatchByIds(List<Long> pIds) {
        if (CollectionUtil.isEmpty(pIds)) {
            return List.of();
        }
        List<Problem> problems = this.listByIds(pIds);
        return BeanUtil.copyToList(problems, ProblemVo.class);

    }

    @Override
    public boolean isExisted(Long problemId) {
        return this.exists(
                new LambdaQueryWrapper<Problem>()
                        .eq(Problem::getProblemId, problemId)
        );
    }




}




