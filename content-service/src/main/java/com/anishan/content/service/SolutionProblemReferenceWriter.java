package com.anishan.content.service;

import com.anishan.content.domain.entity.SolutionProblemReference;
import com.anishan.content.mapper.SolutionProblemReferenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Performs only local projection writes; callers must finish all remote checks first. */
@Service
@RequiredArgsConstructor
public class SolutionProblemReferenceWriter {
    private final SolutionProblemReferenceMapper referenceMapper;

    @Transactional
    public void storeFenced(List<SolutionProblemReference> updates, LocalDateTime issuedAt) {
        for (SolutionProblemReference update : updates) referenceMapper.upsertFenced(update);
        List<Long> ids = updates.stream().map(SolutionProblemReference::getProblemId).collect(Collectors.toList());
        Map<Long, SolutionProblemReference> stored = referenceMapper.selectByProblemIds(ids).stream()
                .collect(Collectors.toMap(SolutionProblemReference::getProblemId, item -> item,
                        (first, ignored) -> first, LinkedHashMap::new));
        for (SolutionProblemReference update : updates) {
            SolutionProblemReference current = stored.get(update.getProblemId());
            if (current == null || current.getRequestedAt() == null
                    || !current.getRequestedAt().isEqual(issuedAt)
                    || !sameProjection(update, current)) {
                throw new SolutionReferenceFenceConflictException();
            }
        }
    }

    private boolean sameProjection(SolutionProblemReference left, SolutionProblemReference right) {
        return Objects.equals(left.getExistsFlag(), right.getExistsFlag())
                && Objects.equals(left.getDelFlag(), right.getDelFlag())
                && Objects.equals(left.getAuth(), right.getAuth())
                && Objects.equals(left.getTitle(), right.getTitle());
    }
}
