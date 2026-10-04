package com.anishan.content.service;

import com.anishan.content.domain.vo.SolutionCandidate;
import com.anishan.content.domain.vo.SolutionCandidatePage;
import com.anishan.content.domain.vo.SolutionRecord;
import com.anishan.content.mapper.SolutionExplanationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SolutionQueryService {
    private static final int CANDIDATE_BATCH_SIZE = 100;

    private final SolutionExplanationMapper solutionMapper;

    /** Profile is target-scoped, never an admin listing; fetch 51 to detect the 50-item cap. */
    @Transactional(readOnly = true)
    public List<SolutionCandidate> profileBatch(Long targetId, Long viewerId, SolutionCandidate cursor) {
        return solutionMapper.selectCandidates(viewerId, false, !targetId.equals(viewerId),
                targetId, null, 0, 51, cursor);
    }

    @Transactional(readOnly = true)
    public List<SolutionRecord> profileRecords(List<Long> solutionIds) {
        if (solutionIds == null || solutionIds.isEmpty()) return Collections.emptyList();
        return solutionMapper.selectProfileRecordsByIds(solutionIds);
    }

    /** Count and first candidate page share one local snapshot; no remote call occurs in this transaction. */
    @Transactional(readOnly = true)
    public SolutionCandidatePage firstPage(Long viewerId, boolean admin, boolean publicOnly,
                                           Long filterUserId, Long problemId, long offset) {
        long total = solutionMapper.countCandidates(viewerId, admin, publicOnly, filterUserId, problemId);
        List<SolutionCandidate> candidates = solutionMapper.selectCandidates(viewerId, admin, publicOnly,
                filterUserId, problemId, offset, CANDIDATE_BATCH_SIZE, null);
        return new SolutionCandidatePage(total, candidates);
    }

    @Transactional(readOnly = true)
    public List<SolutionCandidate> nextBatch(Long viewerId, boolean admin, boolean publicOnly,
                                             Long filterUserId, Long problemId, SolutionCandidate cursor) {
        return solutionMapper.selectCandidates(viewerId, admin, publicOnly, filterUserId, problemId,
                0, CANDIDATE_BATCH_SIZE, cursor);
    }

    @Transactional(readOnly = true)
    public List<SolutionRecord> records(List<Long> solutionIds) {
        if (solutionIds == null || solutionIds.isEmpty()) return Collections.emptyList();
        return solutionMapper.selectRecordsByIds(solutionIds);
    }

    @Transactional(readOnly = true)
    public SolutionRecord record(Long solutionId) {
        return solutionMapper.selectRecordById(solutionId);
    }

    @Transactional(readOnly = true)
    public SolutionRecord likeSnapshot(Long solutionId) {
        return solutionMapper.selectLikeSnapshot(solutionId);
    }

    /** Metadata-only comment authorization snapshot; never loads the solution body. */
    public SolutionRecord commentSnapshot(Long solutionId) {
        return solutionMapper.selectCommentSnapshot(solutionId);
    }
}
