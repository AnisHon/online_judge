package com.anishan.problem.controller;

import com.anishan.commons.domain.R;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.problem.domain.dto.DetailSolutionDto;
import com.anishan.problem.domain.dto.PagedSolution;
import com.anishan.problem.domain.entity.SolutionExplanation;
import com.anishan.problem.domain.vo.DetailSolutionVo;
import com.anishan.problem.domain.vo.SolutionVo;
import com.anishan.problem.service.SolutionExplanationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api("题解接口")
@RestController
@RequestMapping("/solution")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SolutionController {

    private final SolutionExplanationService solutionExplanationService;

    @GetMapping("/{id}")
    @ApiOperation("普通用户获取题解")
    public R<DetailSolutionVo> get(@RequestHeader("user-id") Long userId, @PathVariable Long id) {
        DetailSolutionVo solutionVo = solutionExplanationService.get(id, userId);
        return R.success(solutionVo);
    }

    @GetMapping("/admin/{id}")
    @ApiOperation("管理员获取题解")
    @PreAuthorize("hasAuthority('problem:solution:list')")
    public R<DetailSolutionVo> get(@PathVariable Long id) {
        DetailSolutionVo solutionVo = solutionExplanationService.adminGet(id);
        return R.success(solutionVo);
    }

    @GetMapping("/list")
    @ApiOperation("普通用户查询题解")
    public R<PagedResult<SolutionVo>> list(@RequestHeader("user-id") Long userId, @Validated PagedSolution pagedSolution) {
        pagedSolution.setUserId(null);
        PagedResult<SolutionVo> pagedResult = solutionExplanationService.pagedQuery(userId, pagedSolution);
        return R.success(pagedResult);
    }

    @GetMapping("/admin/list")
    @ApiOperation("管理员查询题解")
    @PreAuthorize("hasAuthority('problem:solution:list')")
    public R<PagedResult<SolutionVo>> adminList(@RequestHeader("user-id") Long userId, @Validated PagedSolution pagedSolution) {
        PagedResult<SolutionVo> pagedResult = solutionExplanationService.adminPagedQuery(userId, pagedSolution);
        return R.success(pagedResult);
    }

    @PostMapping
    @ApiOperation("普通用户发送题解")
    public R<Boolean> add(@RequestHeader("user-id") Long userId, @Validated @RequestBody DetailSolutionDto detailSolutionDto) {
        boolean b = solutionExplanationService.add(userId, detailSolutionDto);
        return R.success(b);
    }

    @PostMapping("/admin")
    @ApiOperation("管理员发送题解")
    @PreAuthorize("hasAuthority('problem:solution:add')")
    public R<Boolean> adminAdd(@RequestHeader("user-id") Long userId, @Validated @RequestBody DetailSolutionDto detailSolutionDto) {
        boolean b = solutionExplanationService.adminAdd(userId, detailSolutionDto);
        return R.success(b);
    }

    @PutMapping
    @ApiOperation("普通用户更改题解")
    public R<Boolean> update(@RequestHeader("user-id") Long userId, @Validated @RequestBody DetailSolutionDto detailSolutionDto) {
        boolean b = solutionExplanationService.update(userId, detailSolutionDto);
        return R.success(b);
    }

    @PutMapping("/admin")
    @ApiOperation("管理员更改题解")
    @PreAuthorize("hasAuthority('problem:solution:edit')")
    public R<Boolean> adminUpdate(@Validated @RequestBody DetailSolutionDto detailSolutionDto) {
        boolean b = solutionExplanationService.adminUpdate(detailSolutionDto);
        return R.success(b);
    }

    @DeleteMapping("/{ids}")
    @ApiOperation("普通用户删除题解")
    public R<Boolean> delete(@PathVariable List<Long> ids, @RequestHeader("user-id") Long userId) {
        boolean del = solutionExplanationService.delete(ids, userId);
        return R.success(del);
    }

    @DeleteMapping("/admin/{ids}")
    @ApiOperation("管理员删除题解")
    @PreAuthorize("hasAuthority('problem:solution:remove')")
    public R<Boolean> adminDelete(@PathVariable List<Long> ids) {
        boolean b = solutionExplanationService.removeBatchByIds(ids);
        return R.success(b);
    }

    @PutMapping("/admin/topUp/{id}")
    @ApiOperation("管理员置顶题解")
    @PreAuthorize("hasAuthority('problem:solution:edit')")
    public R<Boolean> topUp(@PathVariable Long id) {
        boolean b = solutionExplanationService
                .updateById(
                        new SolutionExplanation()
                                .setSolutionId(id)
                                .setTopUp(true)
                );
        return R.success(b);
    }

    @PutMapping("/admin/lowDown/{id}")
    @ApiOperation("管理员取消置顶题解")
    @PreAuthorize("hasAuthority('problem:solution:edit')")
    public R<Boolean> lowDown(@PathVariable Long id) {
        boolean b = solutionExplanationService
                .updateById(
                        new SolutionExplanation()
                                .setSolutionId(id)
                                .setTopUp(false)
                );
        return R.success(b);
    }
}
