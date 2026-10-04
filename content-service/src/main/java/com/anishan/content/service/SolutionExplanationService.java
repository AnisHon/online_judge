package com.anishan.content.service;

import com.anishan.content.domain.dto.DetailSolutionDto;
import com.anishan.content.domain.dto.PagedSolution;
import com.anishan.content.domain.vo.DetailSolutionVo;
import com.anishan.content.domain.vo.SolutionVo;
import com.anishan.commons.domain.vo.PagedResult;

import java.util.List;

public interface SolutionExplanationService {
    DetailSolutionVo get(Long id, Long userId);
    DetailSolutionVo adminGet(Long id);
    DetailSolutionVo adminGet(Long id, Long viewerId);
    List<SolutionVo> recent();
    List<SolutionVo> recent(Long viewerId);
    boolean delete(List<Long> ids, Long userId);
    boolean adminDelete(List<Long> ids);
    PagedResult<SolutionVo> pagedQuery(Long userId, PagedSolution query);
    PagedResult<SolutionVo> adminPagedQuery(Long userId, PagedSolution query);
    boolean update(Long userId, DetailSolutionDto dto);
    boolean adminUpdate(DetailSolutionDto dto);
    boolean add(Long userId, DetailSolutionDto dto);
    boolean adminAdd(Long userId, DetailSolutionDto dto);
    boolean setTopUp(Long solutionId, boolean topUp);
}
