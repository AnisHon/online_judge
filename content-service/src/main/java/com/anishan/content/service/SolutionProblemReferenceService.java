package com.anishan.content.service;

import com.anishan.api.client.problem.domain.vo.ContentProblemReadVo;
import com.anishan.content.domain.entity.SolutionProblemReference;
import com.anishan.content.mapper.SolutionProblemReferenceMapper;
import com.anishan.commons.exception.ApiStatusException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SolutionProblemReferenceService {
    private final SolutionProblemReferenceMapper referenceMapper;
    private final SolutionAccessService accessService;
    private final SolutionProblemReferenceWriter referenceWriter;
    private final Clock clock = Clock.system(ZoneId.of("Asia/Shanghai"));

    /**
     * Fetches authoritative state first, then conditionally updates the non-authoritative local projection.
     * A response whose request timestamp lost a race is discarded and fetched once more.
     */
    public Map<Long, ContentProblemReadVo> refresh(List<Long> problemIds) {
        List<Long> ids = problemIds.stream().filter(Objects::nonNull).distinct()
                .collect(java.util.stream.Collectors.toList());
        if (ids.isEmpty()) return new LinkedHashMap<>();

        for (int attempt = 0; attempt < 2; attempt++) {
            LocalDateTime issuedAt = nextIssuedAt(ids);
            Map<Long, ContentProblemReadVo> fresh = accessService.readFresh(ids);
            LocalDateTime checkedAt = now();
            List<SolutionProblemReference> updates = new ArrayList<>(ids.size());
            for (Long id : ids) {
                ContentProblemReadVo item = fresh.get(id);
                updates.add(new SolutionProblemReference()
                        .setProblemId(id)
                        .setExistsFlag(item.isExists())
                        .setDelFlag(item.isDeleted())
                        .setAuth(item.getAuth())
                        // A service-identity refresh never persists titles for non-public problems.
                        .setTitle(accessService.isPubliclyReadable(item) ? item.getTitle() : null)
                        .setCheckedAt(checkedAt)
                        .setRequestedAt(issuedAt));
            }
            try {
                referenceWriter.storeFenced(updates, issuedAt);
                return fresh;
            } catch (SolutionReferenceFenceConflictException ignored) {
                // A competing check advanced the projection. Recheck problem-service once before returning.
            }
        }
        throw new ApiStatusException(503, "题目状态正在变化，请稍后重试");
    }

    private LocalDateTime nextIssuedAt(List<Long> ids) {
        LocalDateTime issuedAt = now();
        LocalDateTime latest = referenceMapper.selectByProblemIds(ids).stream()
                .map(SolutionProblemReference::getRequestedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        if (latest != null && !issuedAt.isAfter(latest)) {
            issuedAt = latest.plus(1, ChronoUnit.MILLIS);
        }
        return issuedAt;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
    }

}
