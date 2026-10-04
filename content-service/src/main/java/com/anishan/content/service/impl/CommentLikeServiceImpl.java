package com.anishan.content.service.impl;

import com.anishan.api.client.problem.domain.vo.ContentProblemReadVo;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.content.domain.entity.SolutionComment;
import com.anishan.content.domain.enumeration.CommentState;
import com.anishan.content.domain.enumeration.SolutionModerationState;
import com.anishan.content.domain.vo.LikeResultVo;
import com.anishan.content.domain.vo.SolutionRecord;
import com.anishan.content.mapper.SolutionCommentMapper;
import com.anishan.content.service.CommentLikeLocalWriteService;
import com.anishan.content.service.CommentLikeService;
import com.anishan.content.service.SolutionAccessService;
import com.anishan.content.service.SolutionDomainMigrationGate;
import com.anishan.content.service.SolutionProblemReferenceService;
import com.anishan.content.service.SolutionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/** Performs fresh cross-service qualification before entering the local row-lock transaction. */
@Service
@RequiredArgsConstructor
public class CommentLikeServiceImpl implements CommentLikeService {
    private final SolutionQueryService solutionQueryService;
    private final SolutionProblemReferenceService problemReferenceService;
    private final SolutionAccessService solutionAccessService;
    private final SolutionCommentMapper commentMapper;
    private final CommentLikeLocalWriteService localWriteService;
    private final SolutionDomainMigrationGate migrationGate;

    @Override
    public LikeResultVo like(Long userId, Long commentId) {
        requireRequest(userId, commentId);
        migrationGate.requireCutover();
        SolutionComment comment = commentMapper.selectReplyTarget(commentId);
        if (!validTarget(comment, commentId) || comment.getState() != CommentState.VISIBLE) {
            throw notFound();
        }
        SolutionRecord solution = solutionQueryService.likeSnapshot(comment.getSolutionId());
        if (solution == null || Boolean.TRUE.equals(solution.getDelFlag())) throw notFound();
        ContentProblemReadVo freshProblem = freshProblem(solution, true);
        if (!isPublic(solution, freshProblem)) throw notFound();
        return localWriteService.setState(userId, solution.getSolutionId(), resolveRootId(comment),
                commentId, true, freshProblem);
    }

    @Override
    public LikeResultVo unlike(Long userId, Long commentId) {
        requireRequest(userId, commentId);
        migrationGate.requireCutover();
        SolutionComment comment = commentMapper.selectReplyTarget(commentId);
        if (!validTarget(comment, commentId)) return result(false, null);
        SolutionRecord solution = solutionQueryService.likeSnapshot(comment.getSolutionId());
        if (solution == null) return result(false, null);
        // A fresh problem read only controls count visibility; it never blocks cancellation of an old own like.
        ContentProblemReadVo freshProblem = freshProblem(solution, false);
        return localWriteService.setState(userId, solution.getSolutionId(), resolveRootId(comment),
                commentId, false, freshProblem);
    }

    private ContentProblemReadVo freshProblem(SolutionRecord solution, boolean required) {
        if (solution.getProblemId() == null || solution.getProblemId() <= 0) {
            if (required) throw new ApiStatusException(503, "题目状态暂时无法确认，请稍后重试");
            return null;
        }
        Map<Long, ContentProblemReadVo> fresh = problemReferenceService.refresh(
                Collections.singletonList(solution.getProblemId()));
        ContentProblemReadVo problem = fresh == null ? null : fresh.get(solution.getProblemId());
        if (problem == null && required) {
            throw new ApiStatusException(503, "题目状态暂时无法确认，请稍后重试");
        }
        return problem;
    }

    private boolean isPublic(SolutionRecord solution, ContentProblemReadVo problem) {
        return !Boolean.TRUE.equals(solution.getDelFlag())
                && !Boolean.TRUE.equals(solution.getPrivate_())
                && solution.getModerationState() == SolutionModerationState.NORMAL
                && solutionAccessService.isPubliclyReadable(problem);
    }

    private boolean validTarget(SolutionComment comment, Long commentId) {
        if (comment == null || !Objects.equals(comment.getCommentId(), commentId)
                || comment.getSolutionId() == null || comment.getSolutionId() <= 0) return false;
        boolean root = comment.getRootId() == null && comment.getParentId() == null;
        boolean reply = comment.getRootId() != null && comment.getRootId() > 0
                && comment.getParentId() != null && comment.getParentId() > 0;
        return root || reply;
    }

    private Long resolveRootId(SolutionComment comment) {
        return comment.getRootId() == null ? comment.getCommentId() : comment.getRootId();
    }

    private void requireRequest(Long userId, Long commentId) {
        if (userId == null || userId <= 0) throw new ApiStatusException(401, "请先登录");
        if (commentId == null || commentId <= 0) throw new ApiStatusException(400, "评论ID有误");
    }

    private ApiStatusException notFound() {
        return new ApiStatusException(404, "评论不存在或无权访问");
    }

    private LikeResultVo result(boolean liked, Long count) {
        return new LikeResultVo().setLiked(liked).setLikeCount(count);
    }
}
