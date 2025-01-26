package com.anishan.problem.controller;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.anishan.api.client.user.domain.vo.UserVo;
import com.anishan.api.util.AuthUtil;
import com.anishan.commons.domain.R;
import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.enumeration.ContestType;
import com.anishan.commons.enumeration.ValidationGroup;
import com.anishan.problem.domain.dto.ContestDto;
import com.anishan.problem.domain.dto.ContestJoinRequest;
import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.domain.entity.SupplementContest;
import com.anishan.problem.domain.entity.UserContestRelation;
import com.anishan.problem.domain.entity.UserSubmit;
import com.anishan.problem.domain.vo.ContestJoinResponse;
import com.anishan.problem.domain.vo.ContestVo;
import com.anishan.problem.domain.vo.ProblemInListVo;
import com.anishan.problem.domain.vo.SupplementContestVo;
import com.anishan.problem.service.ContestService;
import com.anishan.api.util.CacheUtil;
import com.anishan.problem.service.UserContestService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Api("比赛相关接口")
@RequestMapping("/contest")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ContestController {

    private final ContestService contestService;
    private final UserContestService userContestService;

    @GetMapping("/{id}")
    @ApiOperation("通过id获取比赛")



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

    @GetMapping("/page")
    @ApiOperation("分页获取contest,没有详细信息")
    public R<PagedResult<ContestVo>> listContests(@Validated PagedQuery<Contest> pagedQuery, @NotNull ContestType type) {
        PagedResult<ContestVo> contestVoPagedResult = contestService.listContests(pagedQuery, type);
        return contestVoPagedResult.toR();
    }

    @GetMapping("/adminPage")
    @ApiOperation("分页获取contest,管理用,有详细信息")
    @PreAuthorize("hasAuthority('problem:contest:list')")
    public R<PagedResult<ContestVo>> listContestsAdmin(@Validated PagedQuery<Contest> pagedQuery,  @NotNull ContestType type) {
        PagedResult<ContestVo> contestVoPagedResult = contestService.listContestsAdmin(pagedQuery, type);
        return contestVoPagedResult.toR();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('problem:contest:edit')")
    @ApiOperation("更新Contest，不能更改contestId")
    public R<Boolean> update(@RequestBody @Validated({ValidationGroup.Update.class}) ContestDto contestDto) {
        contestDto.setType(null);
        contestDto.setUserId(null);
        boolean b = contestService.updateContest(contestDto);
        return R.success(b);
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAuthority('problem:contest:remove')")
    @ApiOperation("删除contest")
    public R<Boolean> removeBatch(@PathVariable @NotNull List<Long> ids) {
        CacheUtil.clearAllCaches("problem:contest:", ids);
        boolean b = contestService.removeBatchByIds(ids);
        return R.success(b);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('problem:contest:add')")
    @ApiOperation("添加contest")
    public R<Boolean> addContest(
            @RequestBody @Validated ContestDto contestDto,
            @RequestHeader("user-id") Long userId) {
        contestDto.setUserId(userId);
        boolean b = contestService.addContest(contestDto);
        return R.success(b);
    }



    @GetMapping("/isJoined/{contestId}")
    @ApiOperation("判断用户是否加入比赛")
    public R<Boolean> isJoined(@RequestHeader("user-id") Long userId, @NotNull @PathVariable Long contestId) {
        boolean b = contestService.isUserJoined(contestId, userId);
        return R.success(b);
    }

    @GetMapping("/status/{contestId}")
    @ApiOperation("判断能否答题")
    public R<Boolean> getStatus(@RequestHeader("user-id") Long userId, @PathVariable Long contestId) {
        boolean b = contestService.getStatus(userId, contestId);
        return R.success(b);
    }

    @PostMapping("/submit/{contestId}")
    @ApiOperation("交卷")
    @Transactional
    public R<Object> submit(@PathVariable Long contestId) {
        Long userId = AuthUtil.getUserId();
        boolean status = contestService.getStatus(userId, contestId);
        if (!status) {
            return R.badRequest("不可以交卷");
        }
        boolean save = Db.save(
                new UserSubmit()
                        .setContestId(contestId)
                        .setUserId(userId)
                        .setSubmitTime(LocalDateTimeUtil.now())
        );
        return R.success(save);
    }

    @PostMapping("/lateSubmission")
    @ApiOperation("设置补交")
    @PreAuthorize("hasAuthority('problem:contest:edit')")
    public R<Boolean> addLateSubmission(@Validated @RequestBody SupplementContest supplementContest) {
        boolean b = contestService.addLateSubmission(supplementContest);
        return R.success(b);
    }

    @GetMapping("/lateSubmission/{contestId}")
    @ApiOperation("获取所有补交信息")
    @PreAuthorize("hasAnyAuthority('problem:contest:list', 'user:user:list')")
    public R<List<SupplementContestVo>> getLateSubmission(@PathVariable Long contestId) {
        List<SupplementContestVo> supplementContests = contestService.getLateSubmission(contestId);
        return R.success(supplementContests);
    }

    @DeleteMapping("/lateSubmission/{contestId}/{userId}")
    @ApiOperation("获取所有补交信息")
    @PreAuthorize("hasAnyAuthority('problem:contest:list', 'user:user:list')")
    public R<Boolean> getLateSubmission(@PathVariable String contestId, @PathVariable String userId) {
        boolean b = Db.remove(
                Wrappers
                        .lambdaQuery(SupplementContest.class)
                        .eq(SupplementContest::getContestId, contestId)
                        .eq(SupplementContest::getUserId, userId)
        );
        return R.success(b);
    }


    @GetMapping("/user/{contestId}")
    @ApiOperation("获取所有参加用户的信息")
    @PreAuthorize("hasAnyAuthority('problem:contest:list', 'user:user:list')")
    public R<List<UserVo>> getJoinedUser(@PathVariable Long contestId) {
        return R.success(contestService.getJoinedUser(contestId));
    }

    @PostMapping("/user/{contestId}/{userIds}")
    @ApiOperation("向比赛添加用户")
    @PreAuthorize("hasAuthority('problem:contest:edit')")
    @Transactional
    public R<Boolean> addUser(@PathVariable Long contestId, @PathVariable List<Long> userIds) {
        List<UserContestRelation> relations = userIds.stream()
                .map(x -> new UserContestRelation(x, contestId))
                .collect(Collectors.toList());

        boolean b = userContestService.saveIgnore(relations);

        return R.success(b);
    }

    @DeleteMapping("/user/{contestId}/{userIds}")
    @ApiOperation("删除比赛用户")
    @PreAuthorize("hasAuthority('problem:contest:edit')")
    public R<Boolean> removeUser(@PathVariable Long contestId, @PathVariable List<Long> userIds) {
        boolean b = contestService.removeUser(contestId, userIds);
        return R.success(b);
    }

    @PostMapping("/user/{contestId}/class/{classId}")
    @ApiOperation("通过班级添加用户")
    @PreAuthorize("hasAuthority('problem:contest:edit')")
    public R<Boolean> addUserByClass(@PathVariable Long contestId, @PathVariable Long classId) {
        boolean b = contestService.addUserByClass(contestId, classId);
        return R.success(b);
    }




    @PostMapping("/join")
    @ApiOperation("用户参加比赛接口")
    public R<ContestJoinResponse> joinContest(
            @RequestHeader("user-id") Long userId,
            @RequestBody @Validated ContestJoinRequest contestJoinRequest) {
        ContestJoinResponse resp = contestService.joinContest(userId, contestJoinRequest);
        return R.success(resp);
    }

    @GetMapping("/problems/{id}")
    @ApiOperation("用户获取比赛题目接口")
    public R<List<ProblemInListVo>> getContestProblems(
            @RequestHeader("user-id") Long userId,
            @PathVariable @NotNull Long id
    ) {
        List<ProblemInListVo> problems = contestService.listProblemInContest(userId, id);
        return R.success(problems);
    }



}