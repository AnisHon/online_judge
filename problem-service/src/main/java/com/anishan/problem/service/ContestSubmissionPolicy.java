package com.anishan.problem.service;

import com.anishan.commons.enumeration.ContestType;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.domain.entity.SupplementContest;

import java.time.LocalDateTime;

/** Shared, side-effect-free submission eligibility rules for OJ acceptance and hand-in. */
public final class ContestSubmissionPolicy {

    private ContestSubmissionPolicy() {
    }

    public static LocalDateTime effectiveDeadline(Contest contest, SupplementContest supplement) {
        LocalDateTime end = contest == null ? null : contest.getEndTime();
        if (end == null || contest.getType() != ContestType.HOMEWORK || supplement == null
                || supplement.getDeadline() == null || supplement.getDeadline().isBefore(end)) {
            return end;
        }
        return supplement.getDeadline();
    }

    public static boolean canSubmit(Contest contest, boolean joined, boolean handedIn,
                                    SupplementContest supplement, LocalDateTime now) {
        if (contest == null || now == null || !joined || handedIn
                || (contest.getType() != ContestType.CONTEST && contest.getType() != ContestType.HOMEWORK)) {
            return false;
        }
        LocalDateTime start = contest.getStartTime();
        LocalDateTime deadline = effectiveDeadline(contest, supplement);
        return start != null && deadline != null && start.isBefore(deadline)
                && !now.isBefore(start) && now.isBefore(deadline);
    }

    public static void requireCanSubmit(Contest contest, boolean joined, boolean handedIn,
                                        SupplementContest supplement, LocalDateTime now) {
        if (!joined) {
            throw new ApiStatusException(403, "当前账号不在活动成员范围内");
        }
        if (handedIn) {
            throw new ApiStatusException(409, "已交卷，不能继续提交");
        }
        if (!canSubmit(contest, joined, handedIn, supplement, now)) {
            throw new ApiStatusException(409, "当前不在活动提交时间内");
        }
    }
}
