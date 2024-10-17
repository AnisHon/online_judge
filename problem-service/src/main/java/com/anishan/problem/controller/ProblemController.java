package com.anishan.problem.controller;

import com.anishan.commons.entity.R;
import com.anishan.commons.entity.vo.PagedResult;
import com.anishan.problem.domain.dto.PagedProblem;
import com.anishan.problem.domain.dto.ProblemTagDto;
import com.anishan.problem.domain.vo.DetailProblem;
import com.anishan.problem.domain.vo.ProblemVo;
import com.anishan.problem.service.ProblemService;
import com.anishan.problem.service.TagService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;


@RestController
@RequestMapping("/problem")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Api("题目获取Controller")
public class ProblemController {

    private final ProblemService problemService;
    private final TagService tagService;


    @PostMapping("/list")
    @ApiOperation("条件分页查询题目")
    public R<PagedResult<ProblemVo>> getProblems(@RequestBody @Validated PagedProblem pagedProblem) {
        PagedResult<ProblemVo> problems = problemService.getProblems(pagedProblem);
        return problems.toR();
    }

    @GetMapping("/get/{id}")
    @ApiOperation("通过ID得到详细题目（用于进入题目）")
    public R<DetailProblem> getProblemById(@PathVariable("id") @NotNull Long id) {
        DetailProblem detailProblem = problemService.getDetailProblem(id);
        return R.success(detailProblem);
    }


    @PostMapping("/add-tag")
    @PreAuthorize("hasAuthority('problem:problem:add-tag')")
    @ApiOperation("给题目添加标签")
    public R<Boolean> addTag(ProblemTagDto problemTagDto) {
        boolean b = tagService.addTagForProblem(problemTagDto);
        return R.success(b);
    }

    @PostMapping("/del-tag")
    @PreAuthorize("hasAuthority('problem:problem:del-tag')")
    @ApiOperation("删除某个题目的标签")
    public R<Boolean> delTag(ProblemTagDto problemTagDto) {
        boolean b = tagService.removeTagForProblem(problemTagDto);
        return R.success(b);
    }




}
