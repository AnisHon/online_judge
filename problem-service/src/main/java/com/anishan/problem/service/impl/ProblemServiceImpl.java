package com.anishan.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.anishan.problem.domain.vo.OjProblemVo;
import com.anishan.problem.domain.entity.OjProblemCase;
import com.anishan.api.client.problem.domain.vo.OjProblemCaseVo;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.e.ProblemAuth;
import com.anishan.commons.util.ThrowUtil;
import com.anishan.problem.domain.dto.ChoiceFillAnswersDto;
import com.anishan.problem.domain.dto.DetailProblemDto;
import com.anishan.problem.domain.dto.PagedProblem;
import com.anishan.problem.domain.dto.ProblemDto;
import com.anishan.problem.domain.entity.*;
import com.anishan.problem.domain.vo.*;
import com.anishan.problem.mapper.ProblemMapper;
import com.anishan.problem.mapper.ProblemProblemListMapper;
import com.anishan.problem.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private final OjProblemCaseService ojProblemCaseService;
    private final ProblemProblemListMapper problemProblemListMapper;


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

    public List<Problem> doGetProblems(PagedProblem pagedProblem, Page<Problem> page) {

        return problemMapper
                .selectAllByProblemIdAndTagId(page, pagedProblem.getProblemId(), pagedProblem.getTagIds());
    }

    @Override
    public PagedResult<ProblemVo> getProblems(PagedProblem pagedProblem) {
        Page<Problem> page = pagedProblem.page();
        List<Problem> problems = doGetProblems(pagedProblem, page);
        List<ProblemVo> problemVos = BeanUtil.copyToList(problems, ProblemVo.class);

        return PagedResult.fromPage(pagedProblem.page(), problemVos, page.getTotal());
    }

    @Override
    public PagedResult<ProblemVo> getPagedAll(PagedProblem pagedProblem) {
        Page<Problem> page = pagedProblem.page();
        page = this.page(page, new LambdaQueryWrapper<Problem>()
                .eq(pagedProblem.getProblemId() != null, Problem::getProblemId, pagedProblem.getProblemId())
                .like(!StrUtil.isEmpty(pagedProblem.getTitle()), Problem::getTitle, pagedProblem.getTitle())
                .eq(pagedProblem.getType() != null, Problem::getType, pagedProblem.getType())

        );

        return PagedResult.build(page, ProblemVo.class);
    }

    @Override
    public PagedResult<TaggedProblemVo> listTaggerProblems(PagedProblem pagedProblem) {
        Page<Problem> page = pagedProblem.page();
        Integer type = null;
        if (pagedProblem.getType() != null) {
            type = pagedProblem.getType().getValue();
        }
        List<TaggedProblemVo> taggedProblemVos = problemMapper
                .selectTaggedProblemByProblemIdAndTagId(
                        page,
                        pagedProblem.getProblemId(),
                        pagedProblem.getTagIds(),
                        pagedProblem.getTitle(),
                        type
                );
//        Long l = problemMapper.selectTaggedProblemCountByProblemIdAndTagId(
//                pagedProblem.getProblemId(),
//                pagedProblem.getTagIds(),
//                pagedProblem.getTitle(),
//                type
//        );

        return PagedResult.fromPage(page, taggedProblemVos, page.getTotal());
    }

    // 返回OJ ID
    private Long doAddOjProblem(DetailProblemDto problem) {

        OjProblem ojEntity =
                BeanUtil.copyProperties(problem.getOjProblem(), OjProblem.class);

        boolean save = ojProblemService.save(ojEntity);
        ThrowUtil.runtime(!save, "OJ题目添加失败");

        return ojEntity.getProblemId();


    }

    private boolean doAddOjCases(DetailProblemDto problem, Long problemId) {
        if (!CollectionUtil.isEmpty(problem.getCases())) {
            return true;
        }
        List<OjProblemCase> entityCase =
                BeanUtil.copyToList(problem.getCases(), OjProblemCase.class);

        entityCase.forEach(x -> x.setProblemId(problemId));

        boolean b = ojProblemCaseService.saveBatch(entityCase);
        ThrowUtil.runtime(!b, "OJ题目测试用例添加失败");
        return b;

    }

    private boolean doAddChoiceFillAnswers(List<ChoiceFillAnswersDto> dtoAnswers, Long problemId) {
        List<ChoiceFillAnswers> answers =
                BeanUtil.copyToList(dtoAnswers, ChoiceFillAnswers.class);
        answers.forEach(x -> x.setProblemId(problemId));
        return choiceFillAnswersService.saveBatch(answers);
    }

    private boolean doAddFillProblem(DetailProblemDto problem, Long problemId) {
        List<ChoiceFillAnswersDto> choices = problem.getChoices();
        boolean b = doAddChoiceFillAnswers(choices, problemId);
        ThrowUtil.runtime(!b, "填空答案添加失败");

        return true;
    }

    private boolean doAddChoiceProblem(DetailProblemDto problem, Long problemId) {
        List<ChoiceFillAnswersDto> choices = problem.getChoices();
        boolean b = doAddChoiceFillAnswers(choices, problemId);
        ThrowUtil.runtime(!b, "单选题答案添加失败");

        return true;
    }

    private boolean doAddMultiChoiceProblem(DetailProblemDto problem, Long problemId) {
        List<ChoiceFillAnswersDto> choices = problem.getChoices();
        boolean b = doAddChoiceFillAnswers(choices, problemId);
        ThrowUtil.runtime(!b, "多选题答案添加失败");

        return true;
    }
    private boolean doAddProblem(Problem problem) {

        boolean save = this.save(problem);
        if (!save) {
            throw new RuntimeException("添加失败");
        }
        return true;
    }


    private boolean decidedAddProblem(DetailProblemDto problem) {
        boolean result = false;

        Problem entity = BeanUtil.copyProperties(problem.getProblem(), Problem.class);

        switch (problem.getProblem().getType()) {
            case OJ:
                Long ojId = doAddOjProblem(problem);
                entity.setOjId(ojId);
                result = doAddProblem(entity);
                result &= doAddOjCases(problem, entity.getProblemId());
                break;
            case FILL:
                doAddProblem(entity);
                result = doAddFillProblem(problem, entity.getProblemId());
                break;
            case CHOICE:
                doAddProblem(entity);
                result = doAddChoiceProblem(problem, entity.getProblemId());
                break;
            case MULTI_CHOICE:
                doAddProblem(entity);
                result = doAddMultiChoiceProblem(problem, entity.getProblemId());
                break;
        }

        return result;
    }


    /**
     * 添加problem，开启了事务
     * @param problem 需要添加的problem，需要题目，测试用例，选项，答案
     * @return 返回是否添加成功
     */
    @Override
    @Transactional
    public boolean addProblem(DetailProblemDto problem) {
        return decidedAddProblem(problem);
    }

    private boolean decidedUpdateProblem(DetailProblemDto problem, Problem entity) {
        boolean result = false;



        switch (entity.getType()) {
            case OJ:
                result = doUpdateOjProblem(problem);
                break;
            case FILL:
            case CHOICE:
            case MULTI_CHOICE:
                result = doUpdateFillAnswers(problem.getChoices(), entity.getProblemId());
                break;

        }

        return result;
    }

    private boolean doUpdateFillAnswers(List<ChoiceFillAnswersDto> choices, Long problemId) {
        if (CollectionUtil.isEmpty(choices)) {
            return true;
        }
        List<ChoiceFillAnswers> answers = BeanUtil.copyToList(choices, ChoiceFillAnswers.class);
        answers.forEach(x -> x.setProblemId(problemId));

        List<Long> ids = answers
                .stream()
                .map(ChoiceFillAnswers::getAnswerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        LambdaQueryWrapper<ChoiceFillAnswers> wrapper = new LambdaQueryWrapper<ChoiceFillAnswers>()
                .eq(ChoiceFillAnswers::getProblemId, problemId)
                .notIn(!CollectionUtil.isEmpty(ids), ChoiceFillAnswers::getAnswerId, ids);

        choiceFillAnswersService.remove(
                wrapper
        );


        return choiceFillAnswersService.saveOrUpdateBatch(answers);
    }

    private boolean doUpdateOjProblem(DetailProblemDto problem) {
        problem.getCases().forEach(x -> x.setProblemId(problem.getProblem().getProblemId()));
        boolean caseUpdate = doUpdateOjCases(problem.getCases());
        boolean b = ojProblemService.updateById(problem.getOjProblem());
        return caseUpdate || b;

    }

    private boolean doUpdateOjCases(List<OjProblemCase> cases) {
        if (CollectionUtil.isEmpty(cases)) {
            return true;
        }
        Long problemId = cases.get(0).getProblemId();

        List<Long> ids = cases
                .stream()
                .map(OjProblemCase::getCaseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        LambdaQueryWrapper<OjProblemCase> wrapper = new LambdaQueryWrapper<OjProblemCase>()
                .eq(OjProblemCase::getProblemId, problemId)
                .notIn(!CollectionUtil.isEmpty(ids), OjProblemCase::getCaseId, ids);

        ojProblemCaseService.remove(
                wrapper
        );

        return ojProblemCaseService.saveOrUpdateBatch(cases);
    }

    @Override
    @Transactional
    public boolean updateProblem(DetailProblemDto problem) {
        ProblemDto dto = problem.getProblem();
        Problem entity = BeanUtil.copyProperties(dto, Problem.class);
        ThrowUtil.runtime(entity == null, "题目不存在");

        boolean b = this.updateById(entity);

        return b && decidedUpdateProblem(problem, entity);
    }


    public AdminDetailProblem decidedGetProblem(Problem problem) {

        AdminDetailProblem detailProblem = new AdminDetailProblem();

        ProblemVo problemVo = BeanUtil.copyProperties(problem, ProblemVo.class);

        detailProblem.setProblem(problemVo);

        switch (problem.getType()) {
            case OJ:
                OjProblemVo ojProblemVo =
                        ojProblemService.getOjProblemById(problem.getOjId());
                List<OjProblemCaseVo> cases =
                        ojProblemCaseService.getByProblemId(problem.getProblemId());

                detailProblem.setOjProblem(ojProblemVo);
                detailProblem.setCases(cases);
                break;
            case FILL:
            case CHOICE:
            case MULTI_CHOICE:
                List<ChoiceFillAnswersVo> answers =
                        choiceFillAnswersService.getChoiceVo(problem.getProblemId());
                detailProblem.setChoices(answers);
                break;
        }

        return detailProblem;

    }


    @Override
    public AdminDetailProblem getAdminDetail(Long id) {
        boolean existed = this.isExisted(id);
        ThrowUtil.runtime(!existed, "题目不存在");
        Problem problem = this.getById(id);
        return decidedGetProblem(problem);
    }


    public DetailProblem doGetDetail(ProblemVo problem, Long ojId) {
        if (problem == null) {
            return null;
        }

        List<TagVo> tags = tagService.getTagByProblemId(problem.getProblemId());

        DetailProblem detailProblem = new DetailProblem(problem, null, null, tags, null);
        switch (problem.getType()) {
            case OJ:
                OjProblemVo ojProblem = ojProblemService.getOjProblemById(ojId);
                detailProblem.setOjProblemVo(ojProblem);
                break;
            case FILL:
                Long count = choiceFillAnswersService.countAnswers(problem.getProblemId());
                detailProblem.setCount(count);
                break;
            case MULTI_CHOICE:
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

        if (problemVo == null) {
            return null;
        }
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


    @Override
    public PagedResult<ProblemVo> listProblemNotInList(Long listId, PagedProblem query) {

        List<Long> ids = problemProblemListMapper.selectObjs(
                new LambdaQueryWrapper<ProblemProblemListRelation>()
                        .select(ProblemProblemListRelation::getProblemId)
                        .eq(ProblemProblemListRelation::getListId, listId)
        );

        MPJLambdaWrapper<Problem> wrapper = new MPJLambdaWrapper<Problem>()
                .distinct()
                .selectAll(Problem.class)
                .notIn(!CollectionUtil.isEmpty(ids), Problem::getProblemId, ids)
                .leftJoin(
                        ProblemTagRelation.class,
                        ProblemTagRelation::getProblemId,
                        Problem::getProblemId
                )
                .like(!StrUtil.isEmpty(query.getTitle()), Problem::getTitle, query.getTitle())
                .eq(query.getType() != null, Problem::getType, query.getType())
                .eq(query.getProblemId() != null, Problem::getProblemId, query.getProblemId())
                .in(!CollectionUtil.isEmpty(query.getTagIds()), ProblemTagRelation::getTagId, query.getTagIds());

        Page<ProblemVo> page = query.customPage();
        page = problemMapper.selectJoinPage(page, ProblemVo.class, wrapper);
        return PagedResult.build(page);
    }


}




