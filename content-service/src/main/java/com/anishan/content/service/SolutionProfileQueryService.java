package com.anishan.content.service;

import com.anishan.api.client.problem.domain.vo.ContentProblemReadVo;
import com.anishan.content.domain.enumeration.SolutionModerationState;
import com.anishan.content.domain.vo.*;
import com.anishan.commons.exception.ApiStatusException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/** Uncached content projection. Local transactions finish before each remote eligibility check. */
@Service
@RequiredArgsConstructor
public class SolutionProfileQueryService {
    private final SolutionQueryService queryService;
    private final SolutionProblemReferenceService referenceService;
    private final SolutionAccessService accessService;

    public ProfileSolutionsVo getSolutions(Long targetId, Long viewerId) {
        if (targetId == null || targetId <= 0) throw new ApiStatusException(400, "用户参数有误");
        boolean owner = targetId.equals(viewerId);
        List<ProfileSolutionVo> visible = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        SolutionCandidate cursor = null;
        for (int round = 0; round < 3; round++) {
            List<SolutionCandidate> batch = queryService.profileBatch(targetId, viewerId, cursor);
            if (batch.isEmpty()) return result(targetId, visible);
            Map<Long, ContentProblemReadVo> fresh = referenceService.refresh(batch.stream()
                    .map(SolutionCandidate::getProblemId).distinct().collect(Collectors.toList()));
            Map<Long, SolutionRecord> current = queryService.profileRecords(batch.stream()
                    .map(SolutionCandidate::getSolutionId).collect(Collectors.toList())).stream()
                    .collect(Collectors.toMap(SolutionRecord::getSolutionId, item -> item));
            for (SolutionCandidate candidate : batch) {
                SolutionRecord row = current.get(candidate.getSolutionId());
                if (row == null || Boolean.TRUE.equals(row.getDelFlag())
                        || !targetId.equals(row.getUserId())
                        || !Objects.equals(candidate.getVersion(), row.getVersion())
                        || !Objects.equals(candidate.getProblemId(), row.getProblemId())) continue;
                ContentProblemReadVo problem = fresh.get(row.getProblemId());
                boolean publicReadable = !Boolean.TRUE.equals(row.getPrivate_())
                        && row.getModerationState() == SolutionModerationState.NORMAL
                        && accessService.isPubliclyReadable(problem);
                if ((!owner && !publicReadable) || !seen.add(row.getSolutionId())) continue;
                ProfileSolutionVo item = new ProfileSolutionVo();
                item.setSolutionId(row.getSolutionId());
                item.setProblemId(row.getProblemId());
                item.setTitle(row.getTitle());
                item.setProblemTitle(problem != null && problem.getTitle() != null
                        ? problem.getTitle() : "关联题目不可用");
                item.setPrivate_(Boolean.TRUE.equals(row.getPrivate_()));
                item.setEffectiveVisibility(publicReadable ? "PUBLIC" : "AUTHOR_ONLY");
                item.setCreateTime(row.getCreateTime());
                visible.add(item);
                if (visible.size() == 51) return result(targetId, visible);
            }
            if (batch.size() < 51) return result(targetId, visible);
            cursor = batch.get(batch.size() - 1);
        }
        // Bound repair work; do not silently label an incomplete list as empty or complete.
        if (!queryService.profileBatch(targetId, viewerId, cursor).isEmpty())
            throw new ApiStatusException(503, "题解正在变化，请稍后重试");
        return result(targetId, visible);
    }

    private ProfileSolutionsVo result(Long targetId, List<ProfileSolutionVo> visible) {
        ProfileSolutionsVo result = new ProfileSolutionsVo();
        result.setUserId(targetId);
        result.setSolutionsTruncated(visible.size() > 50);
        result.setSolutions(new ArrayList<>(visible.subList(0, Math.min(50, visible.size()))));
        return result;
    }
}
