package com.anishan.problem.service;

import com.anishan.commons.enumeration.ContestAuth;
import com.anishan.commons.enumeration.ContestType;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.problem.controller.ContestController;
import com.anishan.problem.domain.dto.ContestJoinRequest;
import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.domain.entity.SupplementContest;
import com.anishan.problem.mapper.ContestAttemptMapper;
import com.anishan.problem.mapper.ContestMapper;
import com.anishan.problem.mapper.SupplementContestMapper;
import com.anishan.problem.mapper.UserContestMapper;
import com.anishan.problem.mapper.UserSubmitMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContestParticipationRaceTest {

    private static final Long CONTEST_ID = 991L;
    private static final Long USER_ID = 992L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 10, 0);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-03T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    private final ContestMapper contestMapper = mock(ContestMapper.class);
    private final UserContestMapper userContestMapper = mock(UserContestMapper.class);
    private final UserSubmitMapper userSubmitMapper = mock(UserSubmitMapper.class);
    private final SupplementContestMapper supplementMapper = mock(SupplementContestMapper.class);
    private final ContestAttemptMapper attemptMapper = mock(ContestAttemptMapper.class);
    private ContestParticipationService service;
    private Contest contest;

    @BeforeEach
    void setUp() {
        service = new ContestParticipationService(contestMapper, userContestMapper, userSubmitMapper,
                supplementMapper, attemptMapper, CLOCK);
        contest = contest(ContestType.CONTEST, NOW, NOW.plusHours(1));
        when(contestMapper.selectContestForUpdate(CONTEST_ID)).thenReturn(contest);
        when(userContestMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(userSubmitMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
    }

    @Test
    void handInUsesContestLockAndIsIdempotentOnRepeatedRequests() {
        when(userSubmitMapper.insertIgnore(USER_ID, CONTEST_ID, NOW)).thenReturn(1);
        assertTrue(service.handIn(CONTEST_ID, USER_ID));

        when(userSubmitMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        assertTrue(service.handIn(CONTEST_ID, USER_ID));
        verify(userSubmitMapper, times(1)).insertIgnore(USER_ID, CONTEST_ID, NOW);

        InOrder order = inOrder(contestMapper, userSubmitMapper);
        order.verify(contestMapper).selectContestForUpdate(CONTEST_ID);
        order.verify(userSubmitMapper, times(2)).selectCount(any(Wrapper.class));
        order.verify(userSubmitMapper).insertIgnore(USER_ID, CONTEST_ID, NOW);
        order.verify(contestMapper).selectContestForUpdate(CONTEST_ID);
    }

    @Test
    void handInAllowsStartInstantAndRejectsEndInstantWithHttp409() {
        contest.setStartTime(NOW);
        when(userSubmitMapper.insertIgnore(USER_ID, CONTEST_ID, NOW)).thenReturn(1);
        assertTrue(service.handIn(CONTEST_ID, USER_ID));

        contest.setStartTime(NOW.minusHours(1));
        contest.setEndTime(NOW);
        when(userSubmitMapper.selectCount(any(Wrapper.class))).thenReturn(0L, 0L);
        ApiStatusException error = assertThrows(ApiStatusException.class,
                () -> service.handIn(CONTEST_ID, USER_ID));
        assertEquals(409, error.getStatusCode());
        verify(userSubmitMapper, times(1)).insertIgnore(any(), any(), any());
    }

    @Test
    void homeworkUsesOnlySupplementDeadlineAtOrAfterTheOriginalEnd() {
        contest.setType(ContestType.HOMEWORK);
        contest.setStartTime(NOW.minusHours(2));
        contest.setEndTime(NOW.minusMinutes(10));
        SupplementContest valid = new SupplementContest();
        valid.setDeadline(NOW.plusMinutes(1));
        when(supplementMapper.selectOne(any())).thenReturn(valid);
        when(userSubmitMapper.insertIgnore(USER_ID, CONTEST_ID, NOW)).thenReturn(1);
        assertTrue(service.handIn(CONTEST_ID, USER_ID));

        SupplementContest invalid = new SupplementContest();
        invalid.setDeadline(NOW.minusMinutes(20));
        when(supplementMapper.selectOne(any())).thenReturn(invalid);
        when(userSubmitMapper.selectCount(any(Wrapper.class))).thenReturn(0L, 0L);
        ApiStatusException error = assertThrows(ApiStatusException.class,
                () -> service.handIn(CONTEST_ID, USER_ID));
        assertEquals(409, error.getStatusCode());
    }

    @Test
    void duplicateJoinDoesNotInsertAgainAndKeepsExistingAccessRules() {
        contest.setAuth(ContestAuth.PUBLIC);
        when(userContestMapper.insertBatchIgnore(anyList())).thenReturn(0);
        ContestJoinRequest request = new ContestJoinRequest();
        request.setContestId(CONTEST_ID);
        ContestJoinResponseView response = join(request);
        assertFalse(response.success);

        contest.setAuth(ContestAuth.WhiteList);
        assertFalse(join(request).success);
        verify(userContestMapper, times(1)).insertBatchIgnore(anyList());
    }

    @Test
    void endedContestJoinAndRosterMutationAreRejectedAtExactEnd() {
        contest.setEndTime(NOW);
        ApiStatusException joinError = assertThrows(ApiStatusException.class,
                () -> service.join(USER_ID, joinRequest()));
        assertEquals(409, joinError.getStatusCode());
        ApiStatusException addError = assertThrows(ApiStatusException.class,
                () -> service.addUsers(CONTEST_ID, Collections.singletonList(USER_ID)));
        assertEquals(409, addError.getStatusCode());
        verify(userContestMapper, never()).insertBatchIgnore(anyList());
    }

    @Test
    void removingMemberWithAnyFormalAttemptPreservesRosterAndScores() {
        when(attemptMapper.selectUserIdsWithAttempts(CONTEST_ID, Collections.singletonList(USER_ID)))
                .thenReturn(Collections.singletonList(USER_ID));
        ApiStatusException error = assertThrows(ApiStatusException.class,
                () -> service.removeUsers(CONTEST_ID, Collections.singletonList(USER_ID)));
        assertEquals(409, error.getStatusCode());
        verify(userContestMapper, never()).deleteByContestAndUsers(any(), anyList());
    }

    @Test
    void removalBeforeEndDeletesOnlyRosterRelationAndDeduplicatesBoundedInput() {
        when(attemptMapper.selectUserIdsWithAttempts(CONTEST_ID, Arrays.asList(USER_ID, 993L)))
                .thenReturn(Collections.emptyList());
        when(userContestMapper.deleteByContestAndUsers(CONTEST_ID, Arrays.asList(USER_ID, 993L))).thenReturn(1);
        assertTrue(service.removeUsers(CONTEST_ID, Arrays.asList(USER_ID, USER_ID, 993L)));
        verify(userContestMapper).deleteByContestAndUsers(CONTEST_ID, Arrays.asList(USER_ID, 993L));
        verify(userSubmitMapper, never()).deleteByContestAndUser(any(), any());
    }

    @Test
    void oversizedMemberBatchIsRejectedBeforeTakingContestLock() {
        ApiStatusException error = assertThrows(ApiStatusException.class,
                () -> service.addUsers(CONTEST_ID, Collections.nCopies(501, USER_ID)));
        assertEquals(400, error.getStatusCode());
        verify(contestMapper, never()).selectContestForUpdate(any());
    }

    @Test
    void classBulkAddKeepsTheExistingContestEditPermission() throws Exception {
        PreAuthorize permission = ContestController.class
                .getMethod("addUserByClass", Long.class, Long.class)
                .getAnnotation(PreAuthorize.class);
        assertNotNull(permission);
        assertEquals("hasAuthority('problem:contest:edit')", permission.value());
    }

    @Test
    void handInRetractionIsRejectedAtEndAndDoesNotEraseAttemptData() {
        contest.setEndTime(NOW);
        ApiStatusException error = assertThrows(ApiStatusException.class,
                () -> service.withdrawHandIn(CONTEST_ID, USER_ID));
        assertEquals(409, error.getStatusCode());
        verify(userSubmitMapper, never()).deleteByContestAndUser(any(), any());
        verify(attemptMapper, never()).delete(any());
    }

    private ContestJoinResponseView join(ContestJoinRequest request) {
        com.anishan.problem.domain.vo.ContestJoinResponse response = service.join(USER_ID, request);
        return new ContestJoinResponseView(response.isSuccess(), response.getMessage());
    }

    private ContestJoinRequest joinRequest() {
        ContestJoinRequest request = new ContestJoinRequest();
        request.setContestId(CONTEST_ID);
        request.setPassword("secret");
        return request;
    }

    private Contest contest(ContestType type, LocalDateTime start, LocalDateTime end) {
        Contest value = new Contest();
        value.setContestId(CONTEST_ID);
        value.setType(type);
        value.setAuth(ContestAuth.PUBLIC);
        value.setStartTime(start);
        value.setEndTime(end);
        value.setListId(90L);
        return value;
    }

    private static final class ContestJoinResponseView {
        private final boolean success;
        private final String message;

        private ContestJoinResponseView(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}
