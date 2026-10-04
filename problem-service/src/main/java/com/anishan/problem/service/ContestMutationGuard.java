package com.anishan.problem.service;

import com.anishan.api.domain.entity.OjProblemCase;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.domain.entity.Problem;
import com.anishan.problem.mapper.ContestMapper;
import com.anishan.problem.mapper.ContestMutationReferenceMapper;
import com.anishan.problem.mapper.OjProblemCaseMapper;
import com.anishan.problem.mapper.ProblemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Serializes score-input changes against roster freeze, accepted submissions and active activities.
 * Lock order is contest IDs ascending -> problem IDs ascending -> case IDs ascending. This service
 * must be called in the same database transaction as the mutation it protects.
 */
@Service
public class ContestMutationGuard {

    private final ContestMutationReferenceMapper referenceMapper;
    private final ContestMapper contestMapper;
    private final ProblemMapper problemMapper;
    private final OjProblemCaseMapper caseMapper;
    private final Clock clock;

    @Autowired
    public ContestMutationGuard(ContestMutationReferenceMapper referenceMapper,
                               ContestMapper contestMapper,
                               ProblemMapper problemMapper,
                               OjProblemCaseMapper caseMapper,
                               @Qualifier("contestSubmissionClock") Clock clock) {
        this.referenceMapper = referenceMapper;
        this.contestMapper = contestMapper;
        this.problemMapper = problemMapper;
        this.caseMapper = caseMapper;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public MutationContext lockAndInspect(Collection<Long> requestedProblemIds) {
        List<Long> problemIds = normalizeProblemIds(requestedProblemIds);
        LocalDateTime initialReadAt = LocalDateTime.now(clock);
        Set<Long> initialContestIds = normalizeContestIds(
                referenceMapper.selectBusyContestIds(problemIds, initialReadAt));

        Map<Long, Contest> lockedContests = new TreeMap<>();
        for (Long contestId : initialContestIds) {
            Contest contest = contestMapper.selectContestForUpdate(contestId);
            if (contest != null) {
                lockedContests.put(contestId, contest);
            }
        }

        List<Problem> lockedProblems = problemMapper.selectByProblemIdsForUpdate(problemIds);
        Map<Long, Problem> problemsById = new TreeMap<>();
        if (lockedProblems != null) {
            for (Problem problem : lockedProblems) {
                problemsById.put(problem.getProblemId(), problem);
            }
        }
        if (!problemsById.keySet().containsAll(problemIds)) {
            throw new ApiStatusException(404, "题目不存在或已删除");
        }

        List<OjProblemCase> lockedCases = caseMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OjProblemCase>()
                        .in(OjProblemCase::getProblemId, problemIds)
                        .orderByAsc(OjProblemCase::getCaseId)
                        .last("FOR UPDATE"));

        LocalDateTime recheckAt = LocalDateTime.now(clock);
        Set<Long> currentContestIds = new TreeSet<>();
        currentContestIds.addAll(normalizeContestIds(
                referenceMapper.selectBusySnapshotContestIdsCurrent(problemIds, recheckAt)));
        currentContestIds.addAll(normalizeContestIds(
                referenceMapper.selectBusyListContestIdsCurrent(problemIds, recheckAt)));
        currentContestIds.addAll(normalizeContestIds(
                referenceMapper.selectPendingContestIdsCurrent(problemIds)));

        if (!initialContestIds.containsAll(currentContestIds)) {
            throw new ApiStatusException(409, "活动题目引用刚发生变化，请刷新后重试");
        }

        return new MutationContext(problemIds, problemsById, lockedCases,
                new TreeSet<>(currentContestIds));
    }

    public void requireScoringMutable(MutationContext context) {
        if (context != null && !context.getBusyContestIds().isEmpty()) {
            throw new ApiStatusException(409,
                    "题目正在进行的比赛或作业仍使用当前判分配置，暂不能修改题型、答案或测试用例");
        }
    }

    private List<Long> normalizeProblemIds(Collection<Long> values) {
        if (CollectionUtils.isEmpty(values)) {
            throw new ApiStatusException(400, "题目ID不能为空");
        }
        TreeSet<Long> ids = new TreeSet<>();
        for (Long id : values) {
            if (id == null || id <= 0) {
                throw new ApiStatusException(400, "题目ID无效");
            }
            ids.add(id);
        }
        if (ids.isEmpty()) {
            throw new ApiStatusException(400, "题目ID不能为空");
        }
        return new ArrayList<>(ids);
    }

    private Set<Long> normalizeContestIds(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }
        return values.stream().filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public static final class MutationContext {
        private final List<Long> problemIds;
        private final Map<Long, Problem> problemsById;
        private final List<OjProblemCase> lockedCases;
        private final Set<Long> busyContestIds;

        private MutationContext(List<Long> problemIds,
                                Map<Long, Problem> problemsById,
                                List<OjProblemCase> lockedCases,
                                Set<Long> busyContestIds) {
            this.problemIds = Collections.unmodifiableList(new ArrayList<>(problemIds));
            this.problemsById = Collections.unmodifiableMap(new TreeMap<>(problemsById));
            this.lockedCases = lockedCases == null ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(lockedCases));
            this.busyContestIds = Collections.unmodifiableSet(new TreeSet<>(busyContestIds));
        }

        public List<Long> getProblemIds() {
            return problemIds;
        }

        public Problem getProblem(Long problemId) {
            return problemsById.get(problemId);
        }

        public List<OjProblemCase> getLockedCases() {
            return lockedCases;
        }

        public Set<Long> getBusyContestIds() {
            return busyContestIds;
        }
    }
}
