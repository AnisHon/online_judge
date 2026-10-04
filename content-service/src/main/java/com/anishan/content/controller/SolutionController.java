package com.anishan.content.controller;

import com.anishan.content.domain.dto.DetailSolutionDto;
import com.anishan.content.domain.dto.CommentCreateRequest;
import com.anishan.content.domain.dto.CommentPageQuery;
import com.anishan.content.domain.dto.CommentsStateRequest;
import com.anishan.content.domain.dto.PagedSolution;
import com.anishan.content.domain.vo.CommentVo;
import com.anishan.content.domain.vo.DetailSolutionVo;
import com.anishan.content.domain.vo.SolutionVo;
import com.anishan.content.service.SolutionExplanationService;
import com.anishan.content.service.SolutionModerationService;
import com.anishan.content.service.SolutionLikeService;
import com.anishan.content.service.CommentService;
import com.anishan.content.service.CommentModerationService;
import com.anishan.content.domain.vo.LikeResultVo;
import com.anishan.content.domain.dto.SolutionModerationRequest;
import com.anishan.content.domain.dto.ModerationDeleteRequest;
import com.anishan.content.domain.dto.ModerationPageQuery;
import com.anishan.content.domain.vo.ModerationActionVo;
import com.anishan.api.util.AuthUtil;
import java.util.Map;
import com.anishan.commons.domain.R;
import com.anishan.commons.domain.vo.PagedResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api("题解接口")
@RestController
@RequestMapping("/solution")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SolutionController {
    private final SolutionExplanationService solutionService;
    private final SolutionModerationService moderationService;
    private final SolutionLikeService likeService;
    private final CommentService commentService;
    private final CommentModerationService commentModerationService;

    @GetMapping("/{id}")
    @ApiOperation("普通用户获取题解")
    public R<DetailSolutionVo> get(@PathVariable Long id) {
        return R.success(solutionService.get(id, AuthUtil.getUserId()));
    }

    @GetMapping("/admin/{id}")
    @ApiOperation("管理员获取题解")
    @PreAuthorize("hasAuthority('problem:solution:list')")
    public R<DetailSolutionVo> adminGet(@PathVariable Long id) {
        return R.success(solutionService.adminGet(id, AuthUtil.getUserId()));
    }

    @GetMapping("/list")
    @ApiOperation("普通用户查询题解")
    public R<PagedResult<SolutionVo>> list(@Validated PagedSolution query) {
        return R.success(solutionService.pagedQuery(AuthUtil.getUserId(), query));
    }

    @GetMapping("/admin/list")
    @ApiOperation("管理员查询题解")
    @PreAuthorize("hasAuthority('problem:solution:list')")
    public R<PagedResult<SolutionVo>> adminList(@Validated PagedSolution query) {
        return R.success(solutionService.adminPagedQuery(AuthUtil.getUserId(), query));
    }

    @PostMapping
    @ApiOperation("普通用户发送题解")
    public R<Boolean> add(@Validated @RequestBody DetailSolutionDto dto) {
        return R.success(solutionService.add(AuthUtil.getUserId(), dto));
    }

    @PostMapping("/admin")
    @ApiOperation("管理员发送题解")
    @PreAuthorize("hasAuthority('problem:solution:add')")
    public R<Boolean> adminAdd(@Validated @RequestBody DetailSolutionDto dto) {
        return R.success(solutionService.adminAdd(AuthUtil.getUserId(), dto));
    }

    @PutMapping
    @ApiOperation("普通用户更改题解")
    public R<Boolean> update(@Validated @RequestBody DetailSolutionDto dto) {
        return R.success(solutionService.update(AuthUtil.getUserId(), dto));
    }

    @PutMapping("/admin")
    @ApiOperation("管理员更改题解")
    @PreAuthorize("hasAuthority('problem:solution:edit')")
    public R<Boolean> adminUpdate(@Validated @RequestBody DetailSolutionDto dto) {
        return R.success(solutionService.adminUpdate(dto));
    }

    @DeleteMapping("/{ids}")
    @ApiOperation("普通用户删除题解")
    public R<Boolean> delete(@PathVariable List<Long> ids) {
        return R.success(solutionService.delete(ids, AuthUtil.getUserId()));
    }

    @DeleteMapping("/admin/{ids}")
    @ApiOperation("管理员删除题解")
    @PreAuthorize("hasAuthority('problem:solution:remove')")
    public R<Boolean> adminDelete(@PathVariable List<Long> ids) {
        return R.success(solutionService.adminDelete(ids));
    }

    @PutMapping("/admin/topUp/{id}")
    @ApiOperation("管理员置顶题解")
    @PreAuthorize("hasAuthority('problem:solution:edit')")
    public R<Boolean> topUp(@PathVariable Long id) {
        return R.success(solutionService.setTopUp(id, true));
    }

    @PutMapping("/admin/lowDown/{id}")
    @ApiOperation("管理员取消置顶题解")
    @PreAuthorize("hasAuthority('problem:solution:edit')")
    public R<Boolean> lowDown(@PathVariable Long id) {
        return R.success(solutionService.setTopUp(id, false));
    }

    @GetMapping("/recent")
    @ApiOperation("最近题解")
    public R<List<SolutionVo>> recent() {
        return R.success(solutionService.recent(AuthUtil.getUserId()));
    }

    @PutMapping("/{id}/like")
    @ApiOperation("点赞题解")
    public R<LikeResultVo> like(@PathVariable Long id) {
        return R.success(likeService.like(AuthUtil.getUserId(), id));
    }

    @DeleteMapping("/{id}/like")
    @ApiOperation("取消题解点赞")
    public R<LikeResultVo> unlike(@PathVariable Long id) {
        return R.success(likeService.unlike(AuthUtil.getUserId(), id));
    }

    @GetMapping("/{id}/comments")
    @ApiOperation("分页查询题解一级评论")
    public R<PagedResult<CommentVo>> comments(@PathVariable Long id, @Validated CommentPageQuery query) {
        return R.success(commentService.pageRoots(id, query));
    }

    @PostMapping("/{id}/comments")
    @ApiOperation("创建题解一级评论")
    public R<CommentVo> createComment(@PathVariable Long id,
                                      @Validated @RequestBody CommentCreateRequest request) {
        return R.success(commentService.createRoot(id, request));
    }

    @PutMapping("/{id}/comments-state")
    @ApiOperation("设置题解评论区开关")
    public R<Map<String, Boolean>> setCommentsState(@PathVariable Long id,
                                                    @Validated @RequestBody CommentsStateRequest request) {
        commentModerationService.setCommentsOpen(id, request);
        return R.success(Map.of("commentsOpen", request.getOpen()));
    }

    @PostMapping("/admin/{id}/moderation")
    @PreAuthorize("hasAuthority('problem:solution:edit')")
    public R<Map<String, String>> moderate(@PathVariable Long id,
                                          @Validated @RequestBody SolutionModerationRequest request) {
        return R.success(Map.of("moderationState", moderationService.moderate(id, request).name()));
    }

    @PostMapping("/admin/{id}/delete")
    @PreAuthorize("hasAuthority('problem:solution:remove')")
    public R<Map<String, Boolean>> deleteWithReason(@PathVariable Long id,
                                                   @Validated @RequestBody ModerationDeleteRequest request) {
        moderationService.delete(id, request.getReason());
        return R.success(Map.of("deleted", true));
    }

    @GetMapping("/{id}/moderation")
    public R<List<ModerationActionVo>> ownModeration(@PathVariable Long id) {
        return R.success(moderationService.ownActions(id));
    }

    @GetMapping("/admin/{id}/moderation-history")
    @PreAuthorize("hasAuthority('problem:solution:list')")
    public R<PagedResult<ModerationActionVo>> moderationHistory(@PathVariable Long id,
                                                              @Validated ModerationPageQuery page) {
        return R.success(moderationService.history(id, page));
    }

    // Keep the new command/query API's HTTP status aligned with R.code even for binding/JSON failures.
    @org.springframework.web.bind.annotation.ExceptionHandler({
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.validation.BindException.class})
    public org.springframework.http.ResponseEntity<R<String>> invalidRequest(Exception ignored) {
        return org.springframework.http.ResponseEntity.badRequest().body(R.error(400, "请求参数有误"));
    }
}
