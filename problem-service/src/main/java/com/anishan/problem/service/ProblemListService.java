package com.anishan.problem.service;

import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.problem.domain.dto.PagedProblemList;
import com.anishan.problem.domain.dto.ProblemListDto;
import com.anishan.problem.domain.dto.ProblemListRelationDto;
import com.anishan.problem.domain.entity.ProblemList;
import com.anishan.problem.domain.vo.ProblemInListVo;
import com.anishan.problem.domain.vo.ProblemListVo;
import com.anishan.problem.domain.vo.ProblemVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;
import java.util.List;

/**
* @author happy
* @description 针对表【problem_list(题单表)】的数据库操作Service
* @createDate 2024-10-16 22:39:16
*/
public interface ProblemListService extends IService<ProblemList> {

    List<ProblemVo> getProblemListByListId(Long Id);

    boolean addProblemList(ProblemListDto pl);

    void delProblemList(List<Long> id);

    LocalDateTime getDataTime(Long id);

    boolean updateProblemList(ProblemListDto problemListDto);

    boolean addProblemList(List<ProblemListRelationDto> relations);

    boolean delProblem(List<ProblemListRelationDto> relations);

    List<ProblemInListVo> getProblems(Long id);

    PagedResult<ProblemListVo> listPage(PagedProblemList query);

    List<ProblemInListVo> getProblemsForUser(Long listId);
}
