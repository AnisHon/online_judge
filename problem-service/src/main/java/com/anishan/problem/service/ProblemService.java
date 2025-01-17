package com.anishan.problem.service;

import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.problem.domain.dto.DetailProblemDto;
import com.anishan.problem.domain.dto.PagedProblem;
import com.anishan.problem.domain.entity.Problem;
import com.anishan.problem.domain.vo.AdminDetailProblem;
import com.anishan.problem.domain.vo.ProblemVo;
import com.anishan.problem.domain.vo.DetailProblem;
import com.anishan.problem.domain.vo.TaggedProblemVo;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
* @author happy
* @description 针对表【problem(题目主表，OJ题目有分表，非OJ不需要继续分表)】的数据库操作Service
* @createDate 2024-10-16 22:39:16
*/
public interface ProblemService extends IService<Problem> {

    ProblemVo getProblemById(Long id);

    PagedResult<ProblemVo> getProblems(PagedProblem pagedProblem);

    DetailProblem getDetailProblem(Long id);

    List<ProblemVo> getBatchByIds(List<Long> pIds);

    boolean isExisted(Long problemId);

    PagedResult<ProblemVo> getPagedAll(PagedProblem pagedProblem);

    PagedResult<TaggedProblemVo> listTaggerProblems(PagedProblem pagedProblem);

    Long addProblem(DetailProblemDto problem);

    boolean updateProblem(DetailProblemDto problem);

    AdminDetailProblem getAdminDetail(@NotNull Long id);

    PagedResult<ProblemVo> listProblemNotInList(@NotNull Long listId, PagedProblem query);

    boolean saveProblems(List<DetailProblemDto> problems);

    boolean saveListProblems(List<List<DetailProblemDto>> problems);

    boolean saveMultiParts(MultipartFile[] files);

    List<ProblemVo> recentProblems(@NotNull Integer limit);
}
