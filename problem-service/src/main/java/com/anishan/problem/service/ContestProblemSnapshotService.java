package com.anishan.problem.service;

import com.anishan.problem.domain.entity.ContestProblemSnapshot;

import java.util.List;

/** Internal persistence API; intentionally has no browser controller. */
public interface ContestProblemSnapshotService {
    List<ContestProblemSnapshot> createIfAbsent(Long contestId);

    /** Creates the frozen roster on first authorized read, but never before the activity starts. */
    List<ContestProblemSnapshot> createAfterStart(Long contestId);

    List<ContestProblemSnapshot> getSnapshot(Long contestId);

    List<ContestProblemSnapshot> resetIfNoAttempts(Long contestId, Long newListId);
}
