package com.anishan.problem.service;

import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.problem.domain.dto.DetailSolutionDto;
import com.anishan.problem.domain.dto.PagedSolution;
import com.anishan.problem.domain.entity.SolutionExplanation;
import com.anishan.problem.domain.vo.DetailSolutionVo;
import com.anishan.problem.domain.vo.SolutionVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 文件信息表 服务类
 * </p>
 *
 * @author anishan
 * @since 2025-01-11
 */
public interface SolutionExplanationService extends IService<SolutionExplanation> {

    DetailSolutionVo get(Long id, Long userId);

    DetailSolutionVo adminGet(Long id);

    boolean delete(List<Long> ids, Long userId);

    PagedResult<SolutionVo> pagedQuery(Long userId, PagedSolution pagedSolution);

    PagedResult<SolutionVo> adminPagedQuery(Long userId, PagedSolution pagedSolution);

    boolean update(Long userId, DetailSolutionDto detailSolutionDto);

    boolean adminUpdate(DetailSolutionDto detailSolutionDto);

    boolean add(Long userId, DetailSolutionDto detailSolutionDto);

    boolean adminAdd(Long userId, DetailSolutionDto detailSolutionDto);

}
