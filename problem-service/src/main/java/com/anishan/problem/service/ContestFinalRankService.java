package com.anishan.problem.service;

import com.anishan.problem.domain.ContestRankBuildLease;

import java.util.List;

/** Internal lifecycle API; browser-facing rank APIs are deliberately implemented in T22. */
public interface ContestFinalRankService {

    void initializeForContest(Long contestId);

    void ensureScheduled(Long contestId);

    boolean requestRebuild(Long contestId);

    List<Long> findDueContests(int limit);

    ContestRankBuildLease claim(Long contestId, String leaseOwner);

    boolean renewLease(ContestRankBuildLease lease);

    long buildCandidate(ContestRankBuildLease lease);

    boolean publishCandidate(ContestRankBuildLease lease, long totalUsers);

    void markBuildFailed(ContestRankBuildLease lease, String safeError);

    int cleanupVersionBatch(Long contestId, int batchSize);
}
