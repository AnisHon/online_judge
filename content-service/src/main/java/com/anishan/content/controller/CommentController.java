package com.anishan.content.controller;

import com.anishan.api.util.AuthUtil;
import com.anishan.commons.domain.R;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.content.domain.dto.AdminCommentPageQuery;
import com.anishan.content.domain.dto.CommentPageQuery;
import com.anishan.content.domain.dto.ModerationDeleteRequest;
import com.anishan.content.domain.dto.CommentReplyRequest;
import com.anishan.content.domain.vo.AdminCommentVo;
import com.anishan.content.domain.vo.CommentVo;
import com.anishan.content.domain.vo.LikeResultVo;
import com.anishan.content.domain.vo.ModerationActionVo;
import com.anishan.content.service.CommentLikeService;
import com.anishan.content.service.CommentModerationService;
import com.anishan.content.service.CommentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindException;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

@Api("题解评论接口")
@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CommentController {
    private final CommentService commentService;
    private final CommentLikeService likeService;
    private final CommentModerationService moderationService;

    @PutMapping("/{id}/like")
    @ApiOperation("点赞评论")
    public R<LikeResultVo> like(@PathVariable Long id) {
        return R.success(likeService.like(AuthUtil.getUserId(), id));
    }

    @DeleteMapping("/{id}/like")
    @ApiOperation("取消评论点赞")
    public R<LikeResultVo> unlike(@PathVariable Long id) {
        return R.success(likeService.unlike(AuthUtil.getUserId(), id));
    }

    @GetMapping("/{rootId}/replies")
    @ApiOperation("分页查询一级评论下的回复")
    public R<PagedResult<CommentVo>> replies(@PathVariable Long rootId,
                                             @Validated CommentPageQuery query) {
        return R.success(commentService.pageReplies(rootId, query));
    }

    @PostMapping("/{parentId}/replies")
    @ApiOperation("回复一级评论或已有回复")
    public R<CommentVo> reply(@PathVariable Long parentId,
                              @Validated @RequestBody CommentReplyRequest request) {
        return R.success(commentService.createReply(parentId, request));
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除自己的评论")
    public R<Map<String, Boolean>> deleteOwn(@PathVariable Long id) {
        moderationService.deleteOwn(id);
        return R.success(Map.of("deleted", true));
    }

    @PostMapping("/admin/{id}/delete")
    @PreAuthorize("hasAuthority('problem:comment:remove')")
    @ApiOperation("管理员删除评论")
    public R<Map<String, Boolean>> deleteAsAdmin(@PathVariable Long id,
                                                 @Validated @RequestBody ModerationDeleteRequest request) {
        moderationService.deleteAsAdmin(id, request.getReason());
        return R.success(Map.of("deleted", true));
    }

    @GetMapping("/admin/page")
    @PreAuthorize("hasAuthority('problem:comment:list')")
    @ApiOperation("后台分页查询评论和处理信息")
    public R<PagedResult<AdminCommentVo>> adminPage(@Validated AdminCommentPageQuery query) {
        return R.success(moderationService.pageAdmin(query));
    }

    @GetMapping("/{id}/moderation")
    @ApiOperation("查询本人评论处理结果")
    public R<List<ModerationActionVo>> ownModeration(@PathVariable Long id) {
        return R.success(moderationService.ownActions(id));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class, BindException.class})
    public ResponseEntity<R<String>> invalidRequest(Exception ignored) {
        return ResponseEntity.badRequest().body(R.error(400, "请求参数有误"));
    }
}
