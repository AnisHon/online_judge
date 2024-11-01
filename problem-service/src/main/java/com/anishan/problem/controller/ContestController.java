package com.anishan.problem.controller;

import com.anishan.commons.domain.R;
import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.e.ValidationGroup;
import com.anishan.problem.domain.dto.ContestDto;
import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.domain.vo.ContestVo;
import com.anishan.problem.service.ContestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@Api("比赛相关接口")
@RequestMapping("/contest")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ContestController {



    private final ContestService contestService;

    @GetMapping("/get/{id}")
    @PreAuthorize("hasAuthority('problem:contest:list')")
    @ApiOperation("通过id获取班级")
    public R<ContestVo> getContestById(@PathVariable("id") @NotNull(message = "id为Null") Long id) {
        ContestVo clazz = contestService.getContestById(id);
        return R.success(clazz);
    }

    @GetMapping("/list/{ids}")
    @ApiOperation("通过多个id获取contest，id之间用','隔开")
    public R<List<ContestVo>> listContest(@PathVariable("ids") List<Long> ids) {
        List<ContestVo> contests = contestService.listContestById(ids);
        return R.success(contests);
    }

    @PostMapping("/page")
    @ApiOperation("分页获取contest,没有详细信息")
    public R<PagedResult<ContestVo>> listContests(@RequestBody @Validated PagedQuery<Contest> pagedQuery) {
        PagedResult<ContestVo> contestVoPagedResult = contestService.listContests(pagedQuery);
        return contestVoPagedResult.toR();
    }

    @PostMapping("/admin-page")
    @ApiOperation("分页获取contest,管理用,有详细信息")
    @PreAuthorize("hasAuthority('problem:contest:list')")
    public R<PagedResult<ContestVo>> listContestsAdmin(@RequestBody @Validated PagedQuery<Contest> pagedQuery) {
        PagedResult<ContestVo> contestVoPagedResult = contestService.listContestsAdmin(pagedQuery);
        return contestVoPagedResult.toR();
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('problem:contest:edit')")
    @ApiOperation("更新Contest，不能更改contestId")
    public R<Boolean> update(@RequestBody @Validated({ValidationGroup.Update.class}) ContestDto contestDto) {
        boolean b = contestService.updateContest(contestDto);
        return R.success(b);
    }

    @GetMapping("/remove/{id}")
    @PreAuthorize("hasAuthority('problem:contest:remove')")
    @ApiOperation("删除Contest")
    public R<Boolean> remove(@PathVariable @NotNull Long id) {
        boolean b = contestService.removeById(id);
        return R.success(b);
    }

    @GetMapping("/removeBatch/{ids}")
    @PreAuthorize("hasAuthority('problem:contest:remove')")
    @ApiOperation("删除contest")
    public R<Boolean> removeBatch(@PathVariable @NotNull List<Long> ids) {
        boolean b = contestService.removeBatchByIds(ids);
        return R.success(b);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAuthority('problem:contest:add')")
    @ApiOperation("添加contest")
    public R<Boolean> addContest(
            @RequestBody @Validated ContestDto contestDto,
            @RequestHeader("user-id") Long userId) {
        contestDto.setUserId(userId);
        boolean b = contestService.addContest(contestDto);
        return R.success(b);
    }
    


}