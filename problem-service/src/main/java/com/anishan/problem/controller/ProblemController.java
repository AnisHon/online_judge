package com.anishan.problem.controller;

import com.anishan.commons.entity.R;
import com.anishan.commons.entity.vo.PagedResult;
import com.anishan.problem.domain.dto.PagedProblem;
import com.anishan.problem.domain.vo.DetailProblem;
import com.anishan.problem.domain.vo.ProblemVo;
import com.anishan.problem.service.ProblemService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/problem")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Api("题目获取Controller")
public class ProblemController {

    private final ProblemService problemService;


    @PostMapping("/list")
    public R<PagedResult<ProblemVo>> getProblems(@RequestBody PagedProblem pagedProblem) {
        PagedResult<ProblemVo> problems = problemService.getProblems(pagedProblem);
        return problems.toR();
    }

    @GetMapping("/get/{id}")
    public R<DetailProblem> getProblemById(@PathVariable("id") Long id) {
        DetailProblem detailProblem = problemService.getDetailProblem(id);
        return R.success(detailProblem);
    }




}
