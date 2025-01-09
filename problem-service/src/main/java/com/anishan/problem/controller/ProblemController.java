package com.anishan.problem.controller;

import cn.hutool.core.bean.BeanUtil;
import com.anishan.commons.domain.R;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.enumeration.ProblemAuth;
import com.anishan.commons.enumeration.ValidationGroup;
import com.anishan.problem.domain.dto.DetailProblemDto;
import com.anishan.problem.domain.dto.PagedProblem;
import com.anishan.problem.domain.dto.ProblemTagDto;
import com.anishan.problem.domain.entity.Problem;
import com.anishan.problem.domain.vo.AdminDetailProblem;
import com.anishan.problem.domain.vo.DetailProblem;
import com.anishan.problem.domain.vo.ProblemVo;
import com.anishan.problem.domain.vo.TaggedProblemVo;
import com.anishan.problem.service.ProblemService;
import com.anishan.problem.service.TagService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;


@RestController
@RequestMapping("/problem")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Api("题目获取Controller")
public class ProblemController {

    private final ProblemService problemService;
    private final TagService tagService;


    @PostMapping("/upload")
    @ApiOperation("上传题目")
    @PreAuthorize("hasAuthority('problem:problem:add')")
    public R<Boolean> upload(@RequestParam("file") MultipartFile[] files) {
        boolean b = problemService.saveMultiParts(files);
        return R.success(b);
    }

    @GetMapping("/recentProblems/{limit}")
    @ApiOperation("最近题目，最多50个")
    public R<List<ProblemVo>> getRecentProblems(@NotNull @PathVariable Integer limit) {
        limit = Math.min(limit, 50);

        Page<Problem> page = Page.of(1, limit);
        List<Problem> list = problemService.list(
                page,
                new LambdaQueryWrapper<Problem>()
                        .eq(Problem::getAuth, ProblemAuth.Public)
                        .orderByDesc(Problem::getCreateTime)
        );

        List<ProblemVo> problemVos = BeanUtil.copyToList(list, ProblemVo.class);
        return R.success(problemVos);
    }

    @GetMapping("/list")
    @ApiOperation("条件分页查询题目，这个接口不带标签，无法获取比赛题目")
    public R<PagedResult<ProblemVo>> getProblems(@Validated PagedProblem pagedProblem) {
        PagedResult<ProblemVo> problems = problemService.getProblems(pagedProblem);
        return problems.toR();
    }

    @GetMapping("/taggedList")
    @ApiOperation("条件分页查询题目，这个接口带标签，题目页面用这个就好，无法获取比赛题目")
    public R<PagedResult<TaggedProblemVo>> getTaggedProblems(@Validated PagedProblem pagedProblem) {
        PagedResult<TaggedProblemVo> problems = problemService.listTaggerProblems(pagedProblem);
        return problems.toR();
    }

    @GetMapping("/{id}")
    @ApiOperation("通过ID得到详细题目（用于进入题目）")
    public R<DetailProblem> getProblemById(@PathVariable("id") @NotNull Long id) {
        DetailProblem detailProblem = problemService.getDetailProblem(id);
        return R.success(detailProblem);
    }




    @PutMapping("/delTag")
    @PreAuthorize("hasAuthority('problem:problem:del-tag')")
    @ApiOperation("删除某个题目的标签")
    public R<Boolean> delTag(@RequestBody @Validated(ValidationGroup.Delete.class) ProblemTagDto problemTagDto) {
        boolean b = tagService.removeTagForProblem(problemTagDto);
        return R.success(b);
    }

    @PutMapping("/batchDelTag")
    @PreAuthorize("hasAuthority('problem:problem:del-tag')")
    @ApiOperation("删除某个题目的标签")
    public R<Boolean> batchDelTag(@RequestBody @NotEmpty List<ProblemTagDto> relations) {
        boolean b = tagService.batchRemoveTagsForProblem(relations);
        return R.success(b);
    }

    @PostMapping("/addTag")
    @PreAuthorize("hasAuthority('problem:problem:add-tag')")
    @ApiOperation("给题目添加标签")
    public R<Boolean> addTag(@RequestBody @Validated(ValidationGroup.Insert.class) ProblemTagDto problemTagDto) {
        boolean b = tagService.addTagForProblem(problemTagDto);
        return R.success(b);
    }

    @PostMapping("/batchAddTag")
    @PreAuthorize("hasAuthority('problem:problem:add-tag')")
    @ApiOperation("给题目添加标签")
    public R<Boolean> batchAddTag(@RequestBody @NotEmpty List<ProblemTagDto> relations) {
        boolean b = tagService.batchAddTagsForProblem(relations);
        return R.success(b);
    }




    @GetMapping("/listAll")
    @ApiOperation("管理员查询接口")
    @PreAuthorize("hasAuthority('problem:problem:list')")
    public R<PagedResult<ProblemVo>> getAll(@Validated PagedProblem pagedProblem) {
        PagedResult<ProblemVo> problems = problemService.getPagedAll(pagedProblem);
        return problems.toR();
    }

    @GetMapping("/detail/{id}")
    @ApiOperation("管理员查询详细题目接口")
    @PreAuthorize("hasAuthority('problem:problem:list')")
    public R<AdminDetailProblem> getAdminDetail(@PathVariable @NotNull Long id) {
        AdminDetailProblem problem = problemService.getAdminDetail(id);
        return R.success(problem);
    }

    @PostMapping("/addProblem")
    @ApiOperation("添加题目")
    @PreAuthorize("hasAuthority('problem:problem:add')")
    public R<Long> addProblem(@RequestBody @Validated(ValidationGroup.Insert.class) DetailProblemDto problem) {
        Long b = problemService.addProblem(problem);
        return R.success(b);
    }

    @DeleteMapping("/{ids}")
    @ApiOperation("批量删除题目")
    @PreAuthorize("hasAuthority('problem:problem:remove')")
    public R<Boolean> removeBatchProblem(@PathVariable @NotEmpty List<Long> ids) {
        boolean b = problemService.removeByIds(ids);
        return R.success(b);
    }

    @PutMapping
    @ApiOperation("更新某个题目")
    @PreAuthorize("hasAuthority('problem:problem:edit')")
    public R<Boolean> updateProblem(@RequestBody @Validated(ValidationGroup.Update.class) DetailProblemDto problem) {

        boolean b = problemService.updateProblem(problem);
        return R.success(b);
    }

    @PostMapping("/list-new-problems/{listId}")
    @ApiOperation("获取题单中没有的题目")
    @PreAuthorize("hasAuthority('problem:problem:list')")
    public R<PagedResult<ProblemVo>> listNewProblems(
            @PathVariable @NotNull Long listId,
            @RequestBody @Validated PagedProblem query) {
        PagedResult<ProblemVo> paged = problemService.listProblemNotInList(listId, query);
        return paged.toR();
    }




}
