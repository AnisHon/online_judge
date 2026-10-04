package com.anishan.problem.service;

import com.anishan.api.client.user.client.UserInternalClient;
import com.anishan.api.client.user.domain.dto.UserSummaryRequest;
import com.anishan.api.client.user.domain.vo.UserSummaryVo;
import com.anishan.commons.domain.R;
import com.anishan.commons.enumeration.ContestAuth;
import com.anishan.commons.enumeration.ContestType;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.problem.controller.ContestRankController;
import com.anishan.problem.domain.dto.FinalRankPageQuery;
import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.domain.entity.ContestRankSnapshot;
import com.anishan.problem.domain.enumeration.ContestRankState;
import com.anishan.problem.domain.vo.FinalRankVo;
import com.anishan.problem.mapper.ContestMapper;
import com.anishan.problem.mapper.ContestRankEntryMapper;
import com.anishan.problem.mapper.ContestRankSnapshotMapper;
import com.anishan.problem.mapper.UserContestMapper;
import com.anishan.problem.service.impl.ContestRankQueryServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContestRankPermissionTest {

    private static final Long CONTEST_ID = 9007199254740997L;
    private static final Long USER_ID = 9007199254740993L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 4, 12, 0);

    @Test
    void publicContestIsReadableByLoggedInUserAndLongIdsRemainStrings() throws Exception {
        Fixture fixture = new Fixture();
        fixture.prepare(ContestAuth.PUBLIC, ContestType.CONTEST, NOW.minusMinutes(1), ContestRankState.READY, 7L);
        when(fixture.pageCache.get(anyString(), anyLong(), anyLong(), anyLong(), anyInt(), anyLong())).thenReturn(null);
        ContestRankPageCache.CachedRankEntry cachedRow = fixture.cachedPage(7L).getEntries().get(0);
        com.anishan.problem.domain.entity.ContestRankEntry row = new com.anishan.problem.domain.entity.ContestRankEntry();
        row.setContestId(CONTEST_ID);
        row.setVersion(7L);
        row.setUserId(USER_ID);
        row.setRankNo(cachedRow.getRank());
        row.setRowPosition(cachedRow.getRowPosition());
        row.setScore(new java.math.BigDecimal(cachedRow.getScore()));
        row.setCorrectCount(cachedRow.getCorrectCount());
        row.setAnsweredCount(cachedRow.getAnsweredCount());
        row.setHandedIn(cachedRow.getHandedIn());
        when(fixture.entries.selectPageByVersion(CONTEST_ID, 7L, 0L, 20)).thenReturn(Collections.singletonList(row));
        when(fixture.pageCache.snapshot(org.mockito.ArgumentMatchers.eq(CONTEST_ID),
                org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(20), anyLong(), org.mockito.ArgumentMatchers.anyList()))
                .thenCallRealMethod();

        FinalRankVo result = fixture.service.getPublicRank(CONTEST_ID, USER_ID, fixture.query());

        assertEquals("READY", result.getState());
        assertEquals(1L, result.getRanking().getTotalRecords());
        assertEquals("17.50", result.getRanking().getData().get(0).getScore());
        assertEquals(USER_ID, result.getRanking().getData().get(0).getUserId());
        assertNull(result.getAdminBuildInfo());
        verify(fixture.members, never()).selectCount(any());
        verify(fixture.entries).selectPageByVersion(CONTEST_ID, 7L, 0L, 20);

        JsonNode json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsBytes(result));
        assertEquals(String.valueOf(CONTEST_ID), json.get("contestId").asText());
        assertEquals(String.valueOf(USER_ID), json.at("/ranking/data/0/userId").asText());
        assertEquals("17.50", json.at("/ranking/data/0/score").asText());
        assertFalse(json.toString().contains("email"));
        assertFalse(json.toString().contains("lastError"));
        assertFalse(json.toString().contains("@class"));
    }

    @Test
    void privateAndWhiteListRequireMembershipButAdminPermissionPathDoesNotUseMemberFilter() {
        Fixture fixture = new Fixture();
        fixture.prepare(ContestAuth.PRIVATE, ContestType.CONTEST, NOW.minusMinutes(1), ContestRankState.ERROR, 4L);
        when(fixture.members.selectCount(any())).thenReturn(0L);

        ApiStatusException denied = assertThrows(ApiStatusException.class,
                () -> fixture.service.getPublicRank(CONTEST_ID, USER_ID, fixture.query()));
        assertEquals(404, denied.getStatusCode());
        verify(fixture.snapshots, never()).selectByContestId(CONTEST_ID);

        when(fixture.members.selectCount(any())).thenReturn(1L);
        FinalRankVo joined = fixture.service.getPublicRank(CONTEST_ID, USER_ID, fixture.query());
        assertEquals("ERROR", joined.getState());
        assertEquals("4", joined.getVersion().toString());
        assertTrue(joined.getMessage().contains("最近已发布结果"));
        verify(fixture.members, times(2)).selectCount(any());

        when(fixture.members.selectCount(any())).thenReturn(0L);
        FinalRankVo admin = fixture.service.getAdminRank(CONTEST_ID, fixture.query());
        assertEquals("SIMULATED_FAILURE", admin.getAdminBuildInfo().getLastError());
        verify(fixture.members, times(2)).selectCount(any());

        fixture.prepare(ContestAuth.WhiteList, ContestType.CONTEST, NOW.minusMinutes(1), ContestRankState.READY, 4L);
        when(fixture.members.selectCount(any())).thenReturn(0L);
        ApiStatusException whitelistDenied = assertThrows(ApiStatusException.class,
                () -> fixture.service.getPublicRank(CONTEST_ID, USER_ID, fixture.query()));
        assertEquals(404, whitelistDenied.getStatusCode());
    }

    @Test
    void rejectsAnonymousHomeworkUnendedMissingAndInvalidPagesWithDocumentedStatuses() {
        Fixture fixture = new Fixture();
        ApiStatusException anonymous = assertThrows(ApiStatusException.class,
                () -> fixture.service.getPublicRank(CONTEST_ID, null, fixture.query()));
        assertEquals(401, anonymous.getStatusCode());

        fixture.prepare(ContestAuth.PUBLIC, ContestType.HOMEWORK, NOW.minusMinutes(1), ContestRankState.READY, 1L);
        assertEquals(400, assertThrows(ApiStatusException.class,
                () -> fixture.service.getPublicRank(CONTEST_ID, USER_ID, fixture.query())).getStatusCode());

        fixture.prepare(ContestAuth.PUBLIC, ContestType.CONTEST, NOW.plusMinutes(1), ContestRankState.WAITING, 0L);
        assertEquals(409, assertThrows(ApiStatusException.class,
                () -> fixture.service.getPublicRank(CONTEST_ID, USER_ID, fixture.query())).getStatusCode());

        when(fixture.contests.selectById(55L)).thenReturn(null);
        assertEquals(404, assertThrows(ApiStatusException.class,
                () -> fixture.service.getPublicRank(55L, USER_ID, fixture.query())).getStatusCode());

        FinalRankPageQuery invalid = fixture.query();
        invalid.setPageSize(51L);
        assertEquals(400, assertThrows(ApiStatusException.class,
                () -> fixture.service.getPublicRank(CONTEST_ID, USER_ID, invalid)).getStatusCode());
        invalid.setPageSize(20L);
        invalid.setCurrentPage(Long.MAX_VALUE);
        assertEquals(400, assertThrows(ApiStatusException.class,
                () -> fixture.service.getPublicRank(CONTEST_ID, USER_ID, invalid)).getStatusCode());
    }

    @Test
    void everyPrivateCacheHitRechecksMembershipAndUserSummaryIsNeverCached() {
        Fixture fixture = new Fixture();
        fixture.prepare(ContestAuth.PRIVATE, ContestType.CONTEST, NOW.minusMinutes(1), ContestRankState.BUILDING, 9L);
        when(fixture.members.selectCount(any())).thenReturn(1L);

        FinalRankVo first = fixture.service.getPublicRank(CONTEST_ID, USER_ID, fixture.query());
        FinalRankVo second = fixture.service.getPublicRank(CONTEST_ID, USER_ID, fixture.query());

        assertEquals("BUILDING", first.getState());
        assertEquals("9", second.getVersion().toString());
        verify(fixture.members, times(2)).selectCount(any());
        verify(fixture.pageCache, times(2)).get(anyString(), anyLong(), anyLong(), anyLong(), anyInt(), anyLong());
        verify(fixture.entries, never()).selectPageByVersion(anyLong(), anyLong(), anyLong(), anyInt());
        verify(fixture.users, times(2)).userSummaries(any(UserSummaryRequest.class));
    }

    @Test
    void controllerUsesAuthenticationAndExistingRankPermissionAndRebuildIsIdempotentlyAccepted() throws Exception {
        Method publicMethod = ContestRankController.class.getMethod("getPublicRank", Long.class, FinalRankPageQuery.class);
        Method adminMethod = ContestRankController.class.getMethod("getAdminRank", Long.class, FinalRankPageQuery.class);
        Method rebuildMethod = ContestRankController.class.getMethod("requestRebuild", Long.class);
        assertEquals("isAuthenticated()", publicMethod.getAnnotation(PreAuthorize.class).value());
        assertEquals("hasAuthority('problem:contest:rank')", adminMethod.getAnnotation(PreAuthorize.class).value());
        assertEquals("hasAuthority('problem:contest:rank')", rebuildMethod.getAnnotation(PreAuthorize.class).value());

        ContestRankQueryService service = mock(ContestRankQueryService.class);
        when(service.requestRebuild(CONTEST_ID)).thenReturn(false); // An active build already owns the work.
        R<Map<String, Boolean>> result = new ContestRankController(service).requestRebuild(CONTEST_ID);
        assertEquals(Boolean.TRUE, result.getData().get("accepted"));
        verify(service).requestRebuild(CONTEST_ID);
    }

    private static final class Fixture {
        private final ContestMapper contests = mock(ContestMapper.class);
        private final UserContestMapper members = mock(UserContestMapper.class);
        private final ContestRankSnapshotMapper snapshots = mock(ContestRankSnapshotMapper.class);
        private final ContestRankEntryMapper entries = mock(ContestRankEntryMapper.class);
        private final ContestFinalRankService scheduler = mock(ContestFinalRankService.class);
        private final ContestRankPageCache pageCache = mock(ContestRankPageCache.class);
        private final UserInternalClient users = mock(UserInternalClient.class);
        private final ContestRankQueryServiceImpl service = new ContestRankQueryServiceImpl(
                contests, members, snapshots, entries, scheduler, pageCache, users,
                Clock.fixed(Instant.parse("2026-10-04T04:00:00Z"), ZoneId.of("Asia/Shanghai")));

        private void prepare(ContestAuth auth, ContestType type, LocalDateTime end,
                             ContestRankState state, long version) {
            Contest contest = new Contest();
            contest.setContestId(CONTEST_ID);
            contest.setType(type);
            contest.setAuth(auth);
            contest.setDelFlag(0);
            contest.setEndTime(end);
            when(contests.selectById(CONTEST_ID)).thenReturn(contest);

            ContestRankSnapshot header = new ContestRankSnapshot();
            header.setContestId(CONTEST_ID);
            header.setState(state);
            header.setVersion(version);
            header.setSourceMode("CURRENT");
            header.setRuleVersion("WEIGHTED_LATEST_V1");
            header.setTotalUsers(1L);
            header.setPendingCount(0L);
            header.setBuildAttempts(2);
            header.setNextBuildAt(NOW.plusMinutes(1));
            header.setLeaseUntil(NOW.plusSeconds(20));
            header.setLastError("SIMULATED_FAILURE");
            when(snapshots.selectByContestId(CONTEST_ID)).thenReturn(header);

            ContestRankPageCache.PageSnapshot cached = cachedPage(version);
            when(pageCache.key(CONTEST_ID, version, 1L, 20)).thenReturn("rank-test-page");
            doReturn(cached).when(pageCache).get(anyString(), any(Long.class), any(Long.class),
                    anyLong(), anyInt(), anyLong());
            UserSummaryVo summary = new UserSummaryVo();
            summary.setUserId(USER_ID);
            summary.setUserName("rank-user");
            summary.setNikeName("昵称");
            summary.setSpecialRoles(Collections.singletonList("教师"));
            when(users.userSummaries(any(UserSummaryRequest.class)))
                    .thenReturn(R.success(Collections.singletonList(summary)));
        }

        private FinalRankPageQuery query() {
            FinalRankPageQuery query = new FinalRankPageQuery();
            query.setCurrentPage(1L);
            query.setPageSize(20L);
            return query;
        }

        private ContestRankPageCache.PageSnapshot cachedPage(long version) {
            ContestRankPageCache.PageSnapshot page = new ContestRankPageCache.PageSnapshot();
            page.setContestId(CONTEST_ID);
            page.setVersion(version);
            page.setPage(1L);
            page.setSize(20);
            page.setTotalRecords(1L);
            ContestRankPageCache.CachedRankEntry entry = new ContestRankPageCache.CachedRankEntry();
            entry.setRank(1L);
            entry.setRowPosition(1L);
            entry.setUserId(USER_ID);
            entry.setScore("17.50");
            entry.setCorrectCount(2);
            entry.setAnsweredCount(3);
            entry.setHandedIn(Boolean.TRUE);
            page.setEntries(Collections.singletonList(entry));
            return page;
        }
    }
}
