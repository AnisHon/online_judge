package com.anishan.problem.controller;

import com.anishan.api.util.AuthUtil;
import com.anishan.commons.domain.R;
import com.anishan.problem.domain.dto.FinalRankPageQuery;
import com.anishan.problem.domain.vo.FinalRankVo;
import com.anishan.problem.service.ContestRankQueryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Collections;
import java.util.Map;

@RestController
@Api("比赛最终榜单")
@RequestMapping("/contest")
@Validated
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ContestRankController {

    private final ContestRankQueryService rankQueryService;

    @GetMapping("/{id}/final-rank")
    @PreAuthorize("isAuthenticated()")
    @ApiOperation("查询已结束比赛的最终榜单")
    public R<FinalRankVo> getPublicRank(@PathVariable("id") Long contestId,
                                        @Valid @ModelAttribute FinalRankPageQuery query) {
        return R.success(rankQueryService.getPublicRank(contestId, AuthUtil.getUserId(), query));
    }

    @GetMapping("/admin/{id}/final-rank")
    @PreAuthorize("hasAuthority('problem:contest:rank')")
    @ApiOperation("管理侧查询最终榜单和结算诊断")
    public R<FinalRankVo> getAdminRank(@PathVariable("id") Long contestId,
                                       @Valid @ModelAttribute FinalRankPageQuery query) {
        return R.success(rankQueryService.getAdminRank(contestId, query));
    }

    @PostMapping("/admin/{id}/final-rank/rebuild")
    @PreAuthorize("hasAuthority('problem:contest:rank')")
    @ApiOperation("登记最终榜单重建任务")
    public R<Map<String, Boolean>> requestRebuild(@PathVariable("id") Long contestId) {
        rankQueryService.requestRebuild(contestId);
        return R.success(Collections.singletonMap("accepted", true));
    }
}
