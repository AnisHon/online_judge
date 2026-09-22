package com.anishan.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.anishan.api.client.user.client.UserClient;
import com.anishan.api.client.user.client.UserInternalClient;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.problem.domain.dto.DetailSolutionDto;
import com.anishan.problem.domain.dto.PagedSolution;
import com.anishan.problem.domain.entity.Problem;
import com.anishan.problem.domain.entity.SolutionExplanation;
import com.anishan.problem.domain.entity.SolutionExplanationContent;
import com.anishan.problem.domain.vo.DetailSolutionVo;
import com.anishan.problem.domain.vo.SolutionVo;
import com.anishan.problem.mapper.SolutionExplanationMapper;
import com.anishan.problem.service.SolutionExplanationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.github.yulichang.toolkit.MPJWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * 文件信息表 服务实现类
 * </p>
 *
 * @author anishan
 * @since 2025-01-11
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CacheConfig(cacheNames = "problem:solution:")
public class SolutionExplanationServiceImpl extends ServiceImpl<SolutionExplanationMapper, SolutionExplanation> implements SolutionExplanationService {

    private final SolutionExplanationMapper solutionExplanationMapper;
    private final UserInternalClient userInternalClient;
    private final UserClient userClient;

    @Override
    @Cacheable(key = "'recent-v2'")
    public List<SolutionVo> recent() {
        MPJLambdaWrapper<SolutionExplanation> wrapper = MPJWrappers.lambdaJoin(SolutionExplanation.class)
                .selectAll(SolutionExplanation.class)
                .selectAs(Problem::getTitle, SolutionVo::getProblemTitle)
                .leftJoin(Problem.class, Problem::getProblemId, SolutionExplanation::getProblemId)
                .eq(SolutionExplanation::getPrivate_, false)
                .orderByDesc(SolutionExplanation::getTopUp, SolutionExplanation::getCreateTime)
                .last("LIMIT 20");
        List<SolutionVo> solutions = solutionExplanationMapper.selectJoinList(SolutionVo.class, wrapper);
        fillNikeNames(solutions);
        return solutions;
    }


    public DetailSolutionVo doGet(Long id) {
        MPJLambdaWrapper<SolutionExplanation> wrapper = new MPJLambdaWrapper<SolutionExplanation>()
                .selectAll(SolutionExplanation.class)
                .select(SolutionExplanationContent::getContent)
                .selectAs(Problem::getTitle, SolutionVo::getProblemTitle)
                .leftJoin(SolutionExplanationContent.class, SolutionExplanationContent::getSolutionId, SolutionExplanation::getSolutionId)
                .leftJoin(Problem.class, Problem::getProblemId, SolutionExplanation::getProblemId)
                .eq(SolutionExplanation::getSolutionId, id);

        DetailSolutionVo solution = solutionExplanationMapper.selectJoinOne(DetailSolutionVo.class, wrapper);

        if (solution == null) {
            return null;
        }

        // 获取 id - nikeName 的 map
        Long userId = solution.getUserId();
        Map<Long, String> map = Map.of();
        if (userId != null) {
            var response = userInternalClient.nikeName(List.of(userId));
            if (response != null && response.getData() != null) {
                map = response.getData();
            }
        }

        map
                .values()
                .stream()
                .findFirst()
                .ifPresent(solution::setNikeName);

        return solution;
    }

    /**
     * 普通用户的get方法，
     * @param id solution id
     * @param userId 当前用户ID
     * @return DetailSolutionVo
     */
    @Override
    @Cacheable(key = "#id")
    public DetailSolutionVo get(Long id, Long userId) {

        DetailSolutionVo solution = doGet(id);

        boolean inaccessible =
                solution == null || (solution.getPrivate_() && !Objects.equals(userId, solution.getUserId()));

        if (inaccessible) {
            return null;
        }

        SolutionExplanationContent content = Db.getById(id, SolutionExplanationContent.class);

        return BeanUtil.copyProperties(solution, DetailSolutionVo.class)
                .setContent(content == null ? "" : content.getContent());
    }

    @Override
    public DetailSolutionVo adminGet(Long id) {
        return doGet(id);
    }

    /**
     * 普通用户的delete方法，
     */
    @Override
    public boolean delete(List<Long> ids, Long userId) {
        return solutionExplanationMapper.deleteBatch(ids, userId);
    }

    private  PagedResult<SolutionVo> doQuery(Page<SolutionVo> page, MPJLambdaWrapper<SolutionExplanation> wrapper) {
        // join
        wrapper = wrapper
                .selectAll(SolutionExplanation.class)
                .select(SolutionExplanationContent::getContent)
                .selectAs(Problem::getTitle, SolutionVo::getProblemTitle)
                .leftJoin(SolutionExplanationContent.class, SolutionExplanationContent::getSolutionId, SolutionExplanation::getSolutionId)
                .leftJoin(Problem.class, Problem::getProblemId, SolutionExplanation::getProblemId);
        page = solutionExplanationMapper.selectJoinPage(page, SolutionVo.class, wrapper);

        fillNikeNames(page.getRecords());
        page.getRecords().forEach(x -> {
            if (StrUtil.length(x.getContent()) > 250) {
                x.setContent(StrUtil.sub(x.getContent(), 0, 250));
            }
        });

        return PagedResult.build(page);
    }

    private void fillNikeNames(List<SolutionVo> solutions) {
        List<Long> ids = solutions.stream()
                .map(SolutionVo::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> names = CollUtil.isEmpty(ids)
                ? MapUtil.empty()
                : userInternalClient.nikeName(ids).getData();
        solutions.forEach(solution -> solution.setNikeName(names.get(solution.getUserId())));
    }

    @Override
    public PagedResult<SolutionVo> pagedQuery(Long userId, PagedSolution pagedSolution) {

        Page<SolutionVo> page = pagedSolution.customPage();
        MPJLambdaWrapper<SolutionExplanation> wrapper = MPJWrappers.lambdaJoin(SolutionExplanation.class)
                .eq(pagedSolution.getUserId() != null, SolutionExplanation::getUserId, pagedSolution.getUserId())
                .eq( pagedSolution.getProblemId() !=null, SolutionExplanation::getProblemId, pagedSolution.getProblemId())
                .eq(SolutionExplanation::getPrivate_, false)
                .or(w -> w
                        .eq(SolutionExplanation::getPrivate_, true)
                        .eq(SolutionExplanation::getUserId, userId))
                .orderByDesc(SolutionExplanation::getTopUp);
        return doQuery(page, wrapper);
    }

    @Override
    public PagedResult<SolutionVo> adminPagedQuery(Long userId, PagedSolution pagedSolution) {

        MPJLambdaWrapper<SolutionExplanation> wrapper = MPJWrappers.lambdaJoin(SolutionExplanation.class)
                .eq(pagedSolution.getUserId() != null, SolutionExplanation::getUserId, pagedSolution.getUserId())
                .eq( pagedSolution.getProblemId() !=null, SolutionExplanation::getProblemId, pagedSolution.getProblemId())
                .eq(pagedSolution.getUserId() != null, SolutionExplanation::getUserId, pagedSolution.getUserId())
                .eq( pagedSolution.getProblemId() !=null, SolutionExplanation::getProblemId, pagedSolution.getProblemId());
        Page<SolutionVo> page = pagedSolution.customPage();

        return doQuery(page, wrapper);
    }

    @Override
    @Transactional
    @CacheEvict(key = "#detailSolutionDto.solutionId")
    public boolean update(Long userId, DetailSolutionDto detailSolutionDto) {

        SolutionExplanation solutionExplanation = BeanUtil.copyProperties(detailSolutionDto, SolutionExplanation.class);
        SolutionExplanationContent solutionExplanationContent = new SolutionExplanationContent()
                .setContent(detailSolutionDto.getContent());

        solutionExplanation.setUserId(null);

        boolean b = this.update(solutionExplanation,
                new LambdaQueryWrapper<SolutionExplanation>()
                        .eq(SolutionExplanation::getSolutionId, detailSolutionDto.getSolutionId())
                        .eq(SolutionExplanation::getUserId, userId)
        );

        if (!b) {
            return false;
        }

        solutionExplanationContent.setSolutionId(solutionExplanation.getSolutionId());
        b = Db.saveOrUpdate(solutionExplanationContent);
        return b;
    }

    @Override
    @Transactional
    @CacheEvict(key = "#detailSolutionDto.solutionId")
    public boolean adminUpdate(DetailSolutionDto detailSolutionDto) {
        return saveOrUpdateSolution(null, detailSolutionDto);
    }

    @Override
    @Transactional
    public boolean add(Long userId, DetailSolutionDto detailSolutionDto) {
        detailSolutionDto.setTopUp(false);
        return saveOrUpdateSolution(userId, detailSolutionDto);
    }

    @Override
    @Transactional
    public boolean adminAdd(Long userId, DetailSolutionDto detailSolutionDto) {
        return saveOrUpdateSolution(userId, detailSolutionDto);
    }


    private boolean saveOrUpdateSolution(Long userId, DetailSolutionDto detailSolutionDto) {
        SolutionExplanation solutionExplanation = BeanUtil.copyProperties(detailSolutionDto, SolutionExplanation.class);
        SolutionExplanationContent solutionExplanationContent = new SolutionExplanationContent()
                .setContent(detailSolutionDto.getContent());

        solutionExplanation.setUserId(userId);

        boolean b = this.saveOrUpdate(solutionExplanation);

        solutionExplanationContent.setSolutionId(solutionExplanation.getSolutionId());
        b &= Db.saveOrUpdate(solutionExplanationContent);
        return b;
    }
}
