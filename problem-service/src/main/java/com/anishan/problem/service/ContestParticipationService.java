package com.anishan.problem.service;

import com.anishan.commons.enumeration.ContestAuth;
import com.anishan.commons.enumeration.ContestType;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.problem.domain.dto.ContestJoinRequest;
import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.domain.entity.SupplementContest;
import com.anishan.problem.domain.entity.UserContestRelation;
import com.anishan.problem.domain.entity.UserSubmit;
import com.anishan.problem.mapper.ContestAttemptMapper;
import com.anishan.problem.mapper.ContestMapper;
import com.anishan.problem.mapper.SupplementContestMapper;
import com.anishan.problem.mapper.UserContestMapper;
import com.anishan.problem.mapper.UserSubmitMapper;
import com.anishan.problem.domain.vo.ContestJoinResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Serializes roster and hand-in mutations on the activity row used by OJ acceptance. */
@Service
public class ContestParticipationService {

    public static final int MAX_ROSTER_BATCH_SIZE = 500;

    private final ContestMapper contestMapper;
    private final UserContestMapper userContestMapper;
    private final UserSubmitMapper userSubmitMapper;
    private final SupplementContestMapper supplementContestMapper;
    private final ContestAttemptMapper attemptMapper;
    private final Clock clock;

    @Autowired
    public ContestParticipationService(ContestMapper contestMapper,
                                       UserContestMapper userContestMapper,
                                       UserSubmitMapper userSubmitMapper,
                                       SupplementContestMapper supplementContestMapper,
                                       ContestAttemptMapper attemptMapper,
                                       @Qualifier("contestSubmissionClock") Clock clock) {
        this.contestMapper = contestMapper;
        this.userContestMapper = userContestMapper;
        this.userSubmitMapper = userSubmitMapper;
        this.supplementContestMapper = supplementContestMapper;
        this.attemptMapper = attemptMapper;
        this.clock = clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean handIn(Long contestId, Long userId) {
        requirePositive(contestId, "活动ID无效");
        requirePositive(userId, "用户身份无效");
        Contest contest = lockContest(contestId);

        // Repeated hand-in is idempotent, including after the deadline.
        if (hasHandedIn(contestId, userId)) {
            return true;
        }
        LocalDateTime acceptedAt = LocalDateTime.now(clock);
        assertCanSubmit(contest, userId, acceptedAt);

        int inserted = userSubmitMapper.insertIgnore(userId, contestId, acceptedAt);
        if (inserted == 1 || hasHandedIn(contestId, userId)) {
            return true;
        }
        throw new ApiStatusException(409, "交卷状态已变化，请刷新后重试");
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean withdrawHandIn(Long contestId, Long userId) {
        requirePositive(contestId, "活动ID无效");
        requirePositive(userId, "用户ID无效");
        Contest contest = lockContest(contestId);
        if (contest.getEndTime() == null || !LocalDateTime.now(clock).isBefore(contest.getEndTime())) {
            throw new ApiStatusException(409, "活动结束后不能退回交卷");
        }
        // Deliberately retain contest_attempt and contest_records; a retraction only removes hand-in state.
        return userSubmitMapper.deleteByContestAndUser(contestId, userId) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public ContestJoinResponse join(Long userId, ContestJoinRequest request) {
        requirePositive(userId, "用户身份无效");
        if (request == null) {
            throw new ApiStatusException(400, "加入活动参数无效");
        }
        requirePositive(request.getContestId(), "活动ID无效");
        Contest contest = lockContest(request.getContestId());
        LocalDateTime now = LocalDateTime.now(clock);
        if (contest.getStartTime() == null || contest.getEndTime() == null
                || now.isBefore(contest.getStartTime()) || !now.isBefore(contest.getEndTime())) {
            throw new ApiStatusException(409, "当前不在活动加入时间内");
        }

        ContestAuth auth = contest.getAuth();
        if (auth == ContestAuth.PRIVATE && !java.util.Objects.equals(contest.getPwd(), request.getPassword())) {
            return ContestJoinResponse.fail("密码错误");
        }
        if (auth == ContestAuth.WhiteList) {
            return ContestJoinResponse.fail("无法加入");
        }
        if (auth != ContestAuth.PUBLIC && auth != ContestAuth.PRIVATE) {
            throw new ApiStatusException(409, "活动访问策略无效");
        }

        int inserted = userContestMapper.insertBatchIgnore(java.util.Collections.singletonList(
                new UserContestRelation(userId, request.getContestId())));
        return inserted == 1 ? ContestJoinResponse.success() : ContestJoinResponse.fail("已经加入了");
    }

    public boolean canUserSubmit(Long contestId, Long userId) {
        if (contestId == null || contestId <= 0 || userId == null || userId <= 0) {
            return false;
        }
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            return false;
        }
        boolean joined = isJoined(contestId, userId);
        boolean handedIn = hasHandedIn(contestId, userId);
        SupplementContest supplement = loadSupplement(contestId, userId);
        return ContestSubmissionPolicy.canSubmit(contest, joined, handedIn, supplement, LocalDateTime.now(clock));
    }

    /** Preview check for synchronous non-OJ routes; final OJ admission uses the locked coordinator. */
    public void assertCanSubmitNow(Long contestId, Long userId) {
        requirePositive(contestId, "活动ID无效");
        requirePositive(userId, "用户身份无效");
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new ApiStatusException(404, "活动不存在");
        }
        assertCanSubmit(contest, userId, LocalDateTime.now(clock));
    }

    /** Caller must hold the contest row lock before entering this check. */
    public void assertCanSubmit(Contest contest, Long userId, LocalDateTime acceptedAt) {
        boolean joined = isJoined(contest.getContestId(), userId);
        boolean handedIn = hasHandedIn(contest.getContestId(), userId);
        SupplementContest supplement = loadSupplement(contest.getContestId(), userId);
        ContestSubmissionPolicy.requireCanSubmit(contest, joined, handedIn, supplement, acceptedAt);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean addUsers(Long contestId, Collection<Long> requestedUserIds) {
        requirePositive(contestId, "活动ID无效");
        List<Long> userIds = normalizeUserIds(requestedUserIds);
        if (userIds.isEmpty()) {
            return false;
        }
        Contest contest = lockContest(contestId);
        assertRosterOpen(contest);
        List<UserContestRelation> relations = userIds.stream()
                .map(userId -> new UserContestRelation(userId, contestId))
                .collect(Collectors.toList());
        return userContestMapper.insertBatchIgnore(relations) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean removeUsers(Long contestId, Collection<Long> requestedUserIds) {
        requirePositive(contestId, "活动ID无效");
        List<Long> userIds = normalizeUserIds(requestedUserIds);
        if (userIds.isEmpty()) {
            return false;
        }
        Contest contest = lockContest(contestId);
        assertRosterOpen(contest);
        List<Long> usersWithAttempts = attemptMapper.selectUserIdsWithAttempts(contestId, userIds);
        if (usersWithAttempts != null && !usersWithAttempts.isEmpty()) {
            throw new ApiStatusException(409, "已有正式提交的用户不能移出活动");
        }
        // Scores and hand-in history are intentionally not deleted with the roster relation.
        return userContestMapper.deleteByContestAndUsers(contestId, userIds) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean setSupplement(SupplementContest requested) {
        if (requested == null || requested.getContestId() == null || requested.getContestId() <= 0
                || requested.getUserId() == null || requested.getUserId() <= 0
                || requested.getDeadline() == null) {
            throw new ApiStatusException(400, "补交设置参数无效");
        }
        Contest contest = lockContest(requested.getContestId());
        if (contest.getType() != ContestType.HOMEWORK) {
            throw new ApiStatusException(409, "比赛不支持补交");
        }
        if (contest.getEndTime() == null || requested.getDeadline().isBefore(contest.getEndTime())) {
            throw new ApiStatusException(400, "最迟时间不能早于作业结束时间");
        }
        if (!isJoined(contest.getContestId(), requested.getUserId())) {
            throw new ApiStatusException(409, "用户未参加作业");
        }
        supplementContestMapper.upsertDeadline(requested.getContestId(), requested.getUserId(),
                requested.getDeadline());
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean removeSupplement(Long contestId, Long userId) {
        requirePositive(contestId, "活动ID无效");
        requirePositive(userId, "用户ID无效");
        lockContest(contestId);
        return supplementContestMapper.deleteByContestAndUser(contestId, userId) > 0;
    }

    private Contest lockContest(Long contestId) {
        Contest contest = contestMapper.selectContestForUpdate(contestId);
        if (contest == null) {
            throw new ApiStatusException(404, "活动不存在");
        }
        return contest;
    }

    private void assertRosterOpen(Contest contest) {
        if (contest.getEndTime() == null || !LocalDateTime.now(clock).isBefore(contest.getEndTime())) {
            throw new ApiStatusException(409, "活动结束后不能调整参赛成员");
        }
    }

    private boolean isJoined(Long contestId, Long userId) {
        return userContestMapper.selectCount(new LambdaQueryWrapper<UserContestRelation>()
                .eq(UserContestRelation::getContestId, contestId)
                .eq(UserContestRelation::getUserId, userId)) > 0;
    }

    private boolean hasHandedIn(Long contestId, Long userId) {
        return userSubmitMapper.selectCount(new LambdaQueryWrapper<UserSubmit>()
                .eq(UserSubmit::getContestId, contestId)
                .eq(UserSubmit::getUserId, userId)) > 0;
    }

    private SupplementContest loadSupplement(Long contestId, Long userId) {
        return supplementContestMapper.selectOne(new LambdaQueryWrapper<SupplementContest>()
                .eq(SupplementContest::getContestId, contestId)
                .eq(SupplementContest::getUserId, userId));
    }

    private List<Long> normalizeUserIds(Collection<Long> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        if (values.size() > MAX_ROSTER_BATCH_SIZE) {
            throw new ApiStatusException(400, "一次最多调整500名活动成员");
        }
        Set<Long> unique = new LinkedHashSet<>();
        for (Long userId : values) {
            requirePositive(userId, "用户ID无效");
            unique.add(userId);
        }
        return new ArrayList<>(unique);
    }

    private void requirePositive(Long value, String message) {
        if (value == null || value <= 0) {
            throw new ApiStatusException(400, message);
        }
    }
}
