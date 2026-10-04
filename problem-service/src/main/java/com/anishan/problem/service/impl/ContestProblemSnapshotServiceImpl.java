package com.anishan.problem.service.impl;

import com.anishan.commons.enumeration.ProblemType;
import com.anishan.commons.exception.BusinessException;
import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.domain.entity.ContestProblemSnapshot;
import com.anishan.problem.domain.entity.ContestRankSnapshot;
import com.anishan.problem.domain.enumeration.ContestRankState;
import com.anishan.problem.domain.entity.Problem;
import com.anishan.problem.domain.entity.ProblemProblemListRelation;
import com.anishan.problem.mapper.ContestAttemptMapper;
import com.anishan.problem.mapper.ContestMapper;
import com.anishan.problem.mapper.ContestProblemSnapshotMapper;
import com.anishan.problem.mapper.ContestRankSnapshotMapper;
import com.anishan.problem.mapper.ProblemListMapper;
import com.anishan.problem.mapper.ProblemMapper;
import com.anishan.problem.mapper.ProblemProblemListMapper;
import com.anishan.problem.service.ContestProblemSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Creates immutable roster snapshots without depending on ContestService or RecordsService.
 * Lock order is contest -> problem_list -> problem rows ordered by problem_id.
 */
@Service
public class ContestProblemSnapshotServiceImpl implements ContestProblemSnapshotService {
    private final ContestMapper contestMapper;
    private final ProblemListMapper problemListMapper;
    private final ProblemProblemListMapper problemProblemListMapper;
    private final ProblemMapper problemMapper;
    private final ContestProblemSnapshotMapper snapshotMapper;
    private final ContestRankSnapshotMapper rankSnapshotMapper;
    private final ContestAttemptMapper attemptMapper;
    private final Clock clock;

    @Autowired
    public ContestProblemSnapshotServiceImpl(ContestMapper contestMapper,
                                             ProblemListMapper problemListMapper,
                                             ProblemProblemListMapper problemProblemListMapper,
                                             ProblemMapper problemMapper,
                                             ContestProblemSnapshotMapper snapshotMapper,
                                             ContestRankSnapshotMapper rankSnapshotMapper,
                                             ContestAttemptMapper attemptMapper,
                                             @Qualifier("contestSubmissionClock") Clock clock) {
        this.contestMapper = contestMapper;
        this.problemListMapper = problemListMapper;
        this.problemProblemListMapper = problemProblemListMapper;
        this.problemMapper = problemMapper;
        this.snapshotMapper = snapshotMapper;
        this.rankSnapshotMapper = rankSnapshotMapper;
        this.attemptMapper = attemptMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ContestProblemSnapshot> createIfAbsent(Long contestId) {
        Contest contest = lockContest(contestId);
        return createIfAbsentLocked(contest);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ContestProblemSnapshot> createAfterStart(Long contestId) {
        Contest contest = lockContest(contestId);
        List<ContestProblemSnapshot> existing = snapshotMapper.selectByContestId(contestId);
        if ((!existing.isEmpty() || contest.getProblemSnapshotAt() != null)) {
            return existing;
        }
        if (contest.getStartTime() == null || LocalDateTime.now(clock).isBefore(contest.getStartTime())) {
            throw new com.anishan.commons.exception.ApiStatusException(409, "活动开始后才能查看题目");
        }
        return createIfAbsentLocked(contest);
    }

    private List<ContestProblemSnapshot> createIfAbsentLocked(Contest contest) {
        Long contestId = contest.getContestId();
        List<ContestProblemSnapshot> existing = snapshotMapper.selectByContestId(contestId);
        if (!existing.isEmpty() || contest.getProblemSnapshotAt() != null) {
            return existing;
        }

        lockList(contest.getListId());
        LocalDateTime snapshotAt = LocalDateTime.now(clock);
        List<ContestProblemSnapshot> snapshot = loadValidatedSnapshot(contestId, contest.getListId(), snapshotAt);
        insertSnapshot(snapshot);
        if (contestMapper.setProblemSnapshotAt(contestId, snapshotAt) != 1) {
            throw new BusinessException("比赛题目快照创建失败，请重试");
        }
        return snapshot;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContestProblemSnapshot> getSnapshot(Long contestId) {
        if (contestId == null || contestId <= 0) {
            throw new BusinessException("比赛ID无效");
        }
        return snapshotMapper.selectByContestId(contestId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ContestProblemSnapshot> resetIfNoAttempts(Long contestId, Long newListId) {
        if (newListId == null || newListId <= 0) {
            throw new BusinessException("题单ID无效");
        }
        lockContest(contestId);
        if (attemptMapper.countByContestId(contestId) > 0) {
            throw new BusinessException("比赛已有正式提交，不能重置题目快照");
        }
        ContestRankSnapshot rankSnapshot = rankSnapshotMapper.selectByContestId(contestId);
        if (rankSnapshot != null && (rankSnapshot.getVersion() > 0
                || rankSnapshot.getState() != ContestRankState.WAITING)) {
            throw new BusinessException("比赛榜单已开始生成或已发布，不能重置题目快照");
        }

        lockList(newListId);
        LocalDateTime snapshotAt = LocalDateTime.now(clock);
        List<ContestProblemSnapshot> replacement = loadValidatedSnapshot(contestId, newListId, snapshotAt);
        snapshotMapper.deleteByContestId(contestId);
        if (contestMapper.resetProblemSnapshot(contestId, newListId, snapshotAt) != 1) {
            throw new BusinessException("比赛题目快照重置失败，请重试");
        }
        insertSnapshot(replacement);
        return replacement;
    }

    private Contest lockContest(Long contestId) {
        if (contestId == null || contestId <= 0) {
            throw new BusinessException("比赛ID无效");
        }
        Contest contest = contestMapper.selectContestForUpdate(contestId);
        if (contest == null) {
            throw new BusinessException("比赛不存在");
        }
        if (contest.getListId() == null) {
            throw new BusinessException("比赛未配置题单");
        }
        return contest;
    }

    private void lockList(Long listId) {
        if (listId == null || listId <= 0
                || problemListMapper.selectUpdateTimeForUpdate(listId) == null) {
            throw new BusinessException("题单不存在或已删除");
        }
    }

    private List<ContestProblemSnapshot> loadValidatedSnapshot(Long contestId, Long listId,
                                                                 LocalDateTime snapshotAt) {
        List<ProblemProblemListRelation> relations = problemProblemListMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProblemProblemListRelation>()
                        .eq(ProblemProblemListRelation::getListId, listId)
                        .orderByAsc(ProblemProblemListRelation::getProblemOrder)
                        .orderByAsc(ProblemProblemListRelation::getProblemId));
        if (relations == null || relations.isEmpty()) {
            return new ArrayList<>();
        }

        relations.sort(Comparator
                .comparing(ProblemProblemListRelation::getProblemOrder,
                        Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ProblemProblemListRelation::getProblemId,
                        Comparator.nullsLast(Long::compareTo)));
        List<Long> problemIds = relations.stream()
                .map(ProblemProblemListRelation::getProblemId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        if (problemIds.size() != relations.size()) {
            throw new BusinessException("题单包含无效题目ID，无法创建比赛快照");
        }

        List<Problem> lockedProblems = problemMapper.selectByProblemIdsForUpdate(problemIds);
        Map<Long, Problem> problemById = new HashMap<>();
        if (lockedProblems != null) {
            for (Problem problem : lockedProblems) {
                problemById.put(problem.getProblemId(), problem);
            }
        }
        if (problemById.size() != problemIds.size()) {
            throw new BusinessException("题单包含不存在的题目，无法创建比赛快照");
        }

        List<ContestProblemSnapshot> result = new ArrayList<>(relations.size());
        for (int index = 0; index < relations.size(); index++) {
            ProblemProblemListRelation relation = relations.get(index);
            Problem problem = problemById.get(relation.getProblemId());
            BigDecimal maxScore = relation.getScore();
            if (problem == null || problem.getDelFlag() == null || problem.getDelFlag() != 0
                    || problem.getType() == null || !isSupported(problem.getType())
                    || problem.getTitle() == null || problem.getTitle().trim().isEmpty()
                    || maxScore == null || maxScore.signum() < 0) {
                throw new BusinessException("题单包含已删除或无效题目/分值，无法创建比赛快照");
            }

            ContestProblemSnapshot row = new ContestProblemSnapshot();
            row.setContestId(contestId);
            row.setProblemId(problem.getProblemId());
            // Normalize old duplicate order values while preserving stable order.
            row.setProblemOrder(index + 1);
            row.setTitle(problem.getTitle());
            row.setProblemType(problem.getType().getValue());
            row.setMaxScore(maxScore);
            row.setCreatedAt(snapshotAt);
            result.add(row);
        }
        return result;
    }

    private boolean isSupported(ProblemType type) {
        return type == ProblemType.OJ || type == ProblemType.FILL
                || type == ProblemType.CHOICE || type == ProblemType.MULTI_CHOICE;
    }

    private void insertSnapshot(List<ContestProblemSnapshot> rows) {
        for (ContestProblemSnapshot row : rows) {
            if (snapshotMapper.insert(row) != 1) {
                throw new BusinessException("比赛题目快照保存失败，请重试");
            }
        }
    }
}
