package com.anishan.problem.controller;

import com.anishan.commons.domain.R;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.e.ValidationGroup;
import com.anishan.problem.domain.dto.PagedProblemList;
import com.anishan.problem.domain.dto.ProblemListDto;
import com.anishan.problem.domain.dto.ProblemListRelationDto;
import com.anishan.problem.domain.entity.ProblemProblemListRelation;
import com.anishan.problem.domain.vo.ProblemInListVo;
import com.anishan.problem.domain.vo.ProblemListVo;
import com.anishan.problem.service.ProblemListService;
import com.anishan.problem.service.ProblemProblemListService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/list")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Api("题单接口")
public class ListController {

    private final ProblemListService problemListService;
    private final ProblemProblemListService problemProblemListService;

    @PostMapping("/add")
    @PreAuthorize("hasAuthority('problem:list:add')")
    @ApiOperation("添加题单")
    public R<Boolean> add(@RequestBody @Validated(ValidationGroup.Insert.class) ProblemListDto problemListDto) {
        boolean b = problemListService.addProblemList(problemListDto);
        return R.success(b);
    }

    @GetMapping("/del/{ids}")
    @PreAuthorize("hasAuthority('problem:list:delete')")
    @ApiOperation("删除题单 使用：/del/1,2")
    public R<String> delete(@PathVariable("ids")List<Long> ids) {
        problemListService.delProblemList(ids);
        return R.success();
    }



    @PostMapping("/update")
    @PreAuthorize("hasAuthority('problem:list:update')")
    @ApiOperation("修改题单")
    public R<Boolean> update(@RequestBody @Validated(ValidationGroup.Update.class) ProblemListDto problemListDto) {
        boolean b = problemListService.updateProblem(problemListDto);
        return R.success(b);
    }

    @PostMapping("/add-problem")
    @PreAuthorize("hasAuthority('problem:list:add-problem')")
    @ApiOperation("为题单添加题目")
    public R<Boolean> addProblem(@RequestBody List<ProblemListRelationDto> relations) {
        boolean b = problemListService.addProblem(relations);
        return R.success(b);
    }

    @PostMapping("/update-problem")
    @PreAuthorize("hasAuthority('problem:list:add-problem')")
    @ApiOperation("修改题单题目顺序之类的")
    public R<Boolean> updateProblem(@RequestBody ProblemProblemListRelation relation) {
        boolean update = problemProblemListService.update(
                new LambdaUpdateWrapper<ProblemProblemListRelation>()
                        .set(
                                relation.getScore() != null,
                                ProblemProblemListRelation::getScore,
                                relation.getScore()
                        )
                        .set(relation.getProblemOrder() != null,
                                ProblemProblemListRelation::getProblemOrder,
                                relation.getProblemOrder()
                        )
                        .eq(ProblemProblemListRelation::getProblemId, relation.getProblemId())
                        .eq(ProblemProblemListRelation::getListId, relation.getListId())
        );
        return R.success(update);
    }

    @PostMapping("/del-problem")
    @PreAuthorize("hasAuthority('problem:list:del-problem')")
    @ApiOperation("为题单删除题目")
    public R<Boolean> delProblem(@RequestBody @Validated(ValidationGroup.Delete.class) List<ProblemListRelationDto> relations) {
        boolean b = problemListService.delProblem(relations);
        return R.success(b);
    }

    @GetMapping("/get-problems/{id}")
    @PreAuthorize("hasAuthority('problem:problem:list')")
    @ApiOperation("管理员的根据题单获取题目")
    public R<List<ProblemInListVo>> getProblems(@PathVariable("id") Long id) {
        List<ProblemInListVo> problems = problemListService.getProblems(id);
        return R.success(problems);
    }

    @PostMapping("/problems/{id}}")
    @ApiOperation("用户题单获取，可能由于存在比赛题目题单返回空集合")
    public R<List<ProblemInListVo>> getListProblems(@PathVariable("id") Long id) {
//        todo
        List<ProblemInListVo> problems = problemListService.getProblems(id);
        return R.success(problems);
    }

    @PostMapping("/list")
    @ApiOperation("管理员获取题单")
    @PreAuthorize("hasAuthority('problem:list:list')")
    public R<PagedResult<ProblemListVo>> listProblems(@RequestBody @Validated PagedProblemList query) {
        PagedResult<ProblemListVo> paged = problemListService.listPage(query);
        return paged.toR();
    }




}
