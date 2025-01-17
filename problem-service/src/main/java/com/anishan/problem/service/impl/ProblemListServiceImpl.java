package com.anishan.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.anishan.api.annotation.EnableCache;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.enumeration.ProblemAuth;
import com.anishan.problem.domain.dto.PagedProblemList;
import com.anishan.problem.domain.dto.ProblemListDto;
import com.anishan.problem.domain.dto.ProblemListRelationDto;
import com.anishan.problem.domain.entity.Problem;
import com.anishan.problem.domain.entity.ProblemList;
import com.anishan.problem.domain.entity.ProblemProblemListRelation;
import com.anishan.problem.domain.vo.ProblemInListVo;
import com.anishan.problem.domain.vo.ProblemListVo;
import com.anishan.problem.domain.vo.ProblemVo;
import com.anishan.problem.mapper.ProblemListMapper;
import com.anishan.problem.mapper.ProblemProblemListMapper;
import com.anishan.problem.service.ProblemListService;
import com.anishan.problem.service.ProblemProblemListService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
* @author happy
* @description 针对表【problem_list(题单表)】的数据库操作Service实现
* @createDate 2024-10-16 22:39:16
*/
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ProblemListServiceImpl extends ServiceImpl<ProblemListMapper, ProblemList>
    implements ProblemListService{

    private final ProblemProblemListService problemProblemListService;
    private final ProblemListMapper problemListMapper;
    private final ProblemProblemListMapper problemProblemListMapper;

    @Override
    public List<ProblemVo> getProblemListByListId(Long id) {
        MPJLambdaWrapper<ProblemList> wrapper = new MPJLambdaWrapper<ProblemList>()
                .selectAll(ProblemVo.class)
                .leftJoin(ProblemProblemListRelation.class, ProblemProblemListRelation::getListId, ProblemList::getListId)
                .leftJoin(Problem.class, Problem::getProblemId, ProblemProblemListRelation::getProblemId)
                .eq(Problem::getAuth, ProblemAuth.Public)
                .eq(ProblemList::getListId, id);

        return problemListMapper.selectJoinList(ProblemVo.class, wrapper);
    }

    @Override
    @Transactional
    public boolean addProblemList(ProblemListDto pl) {
        ProblemList problemList = BeanUtil.copyProperties(pl, ProblemList.class);
        return this.save(problemList);
    }

    @Override
    @Transactional
    public void delProblemList(List<Long> ids) {
        if (CollectionUtil.isEmpty(ids)) {
            return;
        }
        this.remove(new LambdaQueryWrapper<ProblemList>()
                .in(ProblemList::getListId, ids)
        );
        problemProblemListService.delByListIds(ids);
    }

    @Override
    public LocalDateTime getDataTime(Long id) {
        return this.getObj(
                new LambdaQueryWrapper<ProblemList>()
                        .select(ProblemList::getUpdateTime)
                        .eq(ProblemList::getListId, id),
                x -> (LocalDateTime) x
        );
    }

    @Override
    @Transactional
    public boolean updateProblemList(ProblemListDto problemListDto) {
        LocalDateTime dataTime = getDataTime(problemListDto.getListId());
        ProblemList problemList = BeanUtil.copyProperties(problemListDto, ProblemList.class);
        problemList.setUpdateTime(dataTime);

        return this.updateById(problemList);
    }

    @Override
    public boolean addProblemList(List<ProblemListRelationDto> relations) {
        List<ProblemProblemListRelation> list = BeanUtil.copyToList(relations, ProblemProblemListRelation.class);

        return problemProblemListService.saveBatch(list);

    }

    @Override
    public boolean delProblem(List<ProblemListRelationDto> relations) {
        if (CollectionUtil.isEmpty(relations)) {
            return true;
        }

        return problemProblemListMapper.deleteBatch(relations) > 0;
    }

    @Override
    public List<ProblemInListVo> getProblems(Long id) {
        MPJLambdaWrapper<ProblemList> wrapper = new MPJLambdaWrapper<ProblemList>()
                .selectAll(Problem.class)
                .select(ProblemProblemListRelation::getProblemOrder, ProblemProblemListRelation::getScore)
                .leftJoin(ProblemProblemListRelation.class, ProblemProblemListRelation::getListId, ProblemList::getListId)
                .leftJoin(Problem.class, Problem::getProblemId, ProblemProblemListRelation::getProblemId)
                .eq(ProblemList::getListId, id)
                .isNotNull(Problem::getProblemId);
        return problemListMapper.selectJoinList(ProblemInListVo.class, wrapper);
    }

    @Override
    public PagedResult<ProblemListVo> listPage(PagedProblemList query) {
        Page<ProblemList> page = query.page();
        Wrapper<ProblemList> wrapper = query.wrapper();

        page = this.page(page, wrapper);

        return PagedResult.build(page, ProblemListVo.class);
    }

    @Override
    @EnableCache(name = "get-contest-problems")
    public List<ProblemInListVo> getProblemsForUser(Long listId) {
        MPJLambdaWrapper<ProblemList> wrapper = new MPJLambdaWrapper<ProblemList>()
                .selectAll(Problem.class)
                .select(ProblemProblemListRelation::getProblemOrder, ProblemProblemListRelation::getScore)
                .leftJoin(ProblemProblemListRelation.class, ProblemProblemListRelation::getListId, ProblemList::getListId)
                .leftJoin(Problem.class, Problem::getProblemId, ProblemProblemListRelation::getProblemId)
                .eq(ProblemList::getListId, listId)
                .eq(Problem::getAuth, ProblemAuth.Public)
                .isNotNull(Problem::getProblemId);
        return problemListMapper.selectJoinList(ProblemInListVo.class, wrapper);
    }
}




