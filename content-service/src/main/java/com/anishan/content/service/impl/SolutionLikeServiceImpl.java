package com.anishan.content.service.impl;

import com.anishan.api.client.problem.domain.vo.ContentProblemReadVo;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.content.domain.vo.LikeResultVo;
import com.anishan.content.domain.vo.SolutionRecord;
import com.anishan.content.mapper.SolutionLikeMapper;
import com.anishan.content.service.SolutionAccessService;
import com.anishan.content.service.SolutionDomainMigrationGate;
import com.anishan.content.service.SolutionLikeLocalWriteService;
import com.anishan.content.service.SolutionLikeService;
import com.anishan.content.service.SolutionProblemReferenceService;
import com.anishan.content.service.SolutionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SolutionLikeServiceImpl implements SolutionLikeService {
    private static final int MAX_BATCH_SIZE = 50;

    private final SolutionQueryService queryService;
    private final SolutionProblemReferenceService referenceService;
    private final SolutionAccessService accessService;
    private final SolutionLikeMapper likeMapper;
    private final SolutionLikeLocalWriteService localWriteService;
    private final SolutionDomainMigrationGate migrationGate;

    @Override
    public LikeResultVo like(Long userId, Long solutionId) {
        requireRequest(userId, solutionId);
        migrationGate.requireCutover();
        SolutionRecord snapshot = queryService.likeSnapshot(solutionId);
        if (snapshot == null || Boolean.TRUE.equals(snapshot.getDelFlag())) {
            throw new ApiStatusException(404, "题解不存在或无权访问");
        }
        ContentProblemReadVo freshProblem = freshProblem(snapshot, true);
        if (!accessService.isPubliclyReadable(freshProblem)) {
            throw new ApiStatusException(404, "题解不存在或无权访问");
        }
        return localWriteService.setState(userId, solutionId, true, freshProblem);
    }

    @Override
    public LikeResultVo unlike(Long userId, Long solutionId) {
        requireRequest(userId, solutionId);
        migrationGate.requireCutover();
        SolutionRecord snapshot = queryService.likeSnapshot(solutionId);
        if (snapshot == null) return new LikeResultVo().setLiked(false).setLikeCount(null);
        // A fresh read is outside the solution/like row-lock transaction. Its PUBLIC bit only
        // controls whether the result may include a count; it never blocks cancellation.
        ContentProblemReadVo freshProblem = freshProblem(snapshot, false);
        return localWriteService.setState(userId, solutionId, false, freshProblem);
    }

    @Override
    public boolean isLiked(Long userId, Long solutionId) {
        if (userId == null || userId <= 0 || solutionId == null || solutionId <= 0) return false;
        return !likedSolutionIds(userId, Collections.singletonList(solutionId)).isEmpty();
    }

    @Override
    public Set<Long> likedSolutionIds(Long userId, Collection<Long> solutionIds) {
        if (userId == null || userId <= 0 || solutionIds == null || solutionIds.isEmpty()) {
            return Collections.emptySet();
        }
        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        for (Long id : solutionIds) {
            if (id != null && id > 0) unique.add(id);
        }
        if (unique.isEmpty()) return Collections.emptySet();
        if (unique.size() > MAX_BATCH_SIZE) {
            throw new ApiStatusException(400, "题解互动状态查询数量过多");
        }
        List<Long> rows = likeMapper.selectActiveSolutionIdsForUser(userId, new ArrayList<>(unique));
        return rows == null ? Collections.emptySet() : new LinkedHashSet<>(rows);
    }

    private ContentProblemReadVo freshProblem(SolutionRecord solution, boolean required) {
        if (solution.getProblemId() == null || solution.getProblemId() <= 0) {
            if (required) throw new ApiStatusException(503, "题目状态暂时无法确认，请稍后重试");
            return null;
        }
        ContentProblemReadVo problem = referenceService.refresh(Collections.singletonList(solution.getProblemId()))
                .get(solution.getProblemId());
        if (problem == null && required) throw new ApiStatusException(503, "题目状态暂时无法确认，请稍后重试");
        return problem;
    }

    private void requireRequest(Long userId, Long solutionId) {
        if (userId == null || userId <= 0) throw new ApiStatusException(401, "请先登录");
        if (solutionId == null || solutionId <= 0) throw new ApiStatusException(400, "题解ID有误");
    }
}
