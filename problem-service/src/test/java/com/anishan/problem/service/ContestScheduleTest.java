package com.anishan.problem.service;

import com.anishan.commons.exception.ApiStatusException;
import com.anishan.problem.domain.dto.ContestDto;
import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.domain.entity.ContestRankSnapshot;
import com.anishan.problem.domain.enumeration.ContestRankState;
import com.anishan.problem.mapper.ContestAttemptMapper;
import com.anishan.problem.mapper.ContestMapper;
import com.anishan.problem.mapper.ContestRankSnapshotMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class ContestScheduleTest {

    private static final Long CONTEST_ID = 701L;
    private static final Long LIST_ID = 702L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 10, 0);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-03T02:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    private final ContestMapper contestMapper = mock(ContestMapper.class);
    private final ContestAttemptMapper attemptMapper = mock(ContestAttemptMapper.class);
    private final ContestRankSnapshotMapper rankMapper = mock(ContestRankSnapshotMapper.class);
    private final ContestProblemSnapshotService snapshotService = mock(ContestProblemSnapshotService.class);
    private ContestScheduleService service;
    private Contest current;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "contest-test"),
                Contest.class);
        service = new ContestScheduleService(contestMapper, attemptMapper, rankMapper, snapshotService, CLOCK);
        current = new Contest();
        current.setContestId(CONTEST_ID);
        current.setListId(LIST_ID);
        current.setStartTime(NOW.minusHours(1));
        current.setEndTime(NOW.plusHours(1));
        when(contestMapper.selectContestForUpdate(CONTEST_ID)).thenReturn(current);
        when(attemptMapper.countByContestId(CONTEST_ID)).thenReturn(0L);
        when(contestMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
    }

    @Test
    void listChangeWithoutAttemptResetsSnapshotAndUpdatesUnderTheSameTransaction() throws Exception {
        ContestDto request = new ContestDto();
        request.setContestId(CONTEST_ID);
        request.setListId(703L);
        request.setStartTime(NOW);
        request.setEndTime(NOW.plusHours(2));
        when(snapshotService.resetIfNoAttempts(CONTEST_ID, 703L)).thenReturn(Collections.emptyList());

        assertTrue(service.update(request));
        verify(contestMapper).selectContestForUpdate(CONTEST_ID);
        verify(snapshotService).resetIfNoAttempts(CONTEST_ID, 703L);
        verify(contestMapper).update(isNull(), any(Wrapper.class));
        Transactional tx = ContestScheduleService.class.getMethod("update", ContestDto.class)
                .getAnnotation(Transactional.class);
        assertNotNull(tx);
        assertArrayEquals(new Class<?>[]{Exception.class}, tx.rollbackFor());
    }

    @Test
    void anyAcceptedAttemptBlocksListAndScheduleChanges() {
        when(attemptMapper.countByContestId(CONTEST_ID)).thenReturn(1L);
        ContestDto request = new ContestDto();
        request.setContestId(CONTEST_ID);
        request.setEndTime(NOW.plusHours(3));
        ApiStatusException error = assertThrows(ApiStatusException.class, () -> service.update(request));
        assertEquals(409, error.getStatusCode());
        verify(contestMapper, never()).update(any(), any(Wrapper.class));
        verify(snapshotService, never()).resetIfNoAttempts(any(), any());
    }

    @Test
    void readyRankMakesScheduleImmutableButAllowsNonScheduleMetadata() {
        ContestRankSnapshot ready = new ContestRankSnapshot();
        ready.setState(ContestRankState.READY);
        ready.setVersion(1L);
        when(rankMapper.selectByContestId(CONTEST_ID)).thenReturn(ready);

        ContestDto schedule = new ContestDto();
        schedule.setContestId(CONTEST_ID);
        schedule.setEndTime(NOW.plusHours(4));
        assertEquals(409, assertThrows(ApiStatusException.class, () -> service.update(schedule)).getStatusCode());

        ContestDto metadata = new ContestDto();
        metadata.setContestId(CONTEST_ID);
        metadata.setTitle("updated title");
        assertTrue(service.update(metadata));
        verify(contestMapper, times(1)).update(isNull(), any(Wrapper.class));
    }

    @Test
    void changingOnlyMetadataDoesNotResetSnapshotOrRequireAttemptInspection() {
        ContestDto request = new ContestDto();
        request.setContestId(CONTEST_ID);
        request.setTitle("new title");
        assertTrue(service.update(request));
        verify(attemptMapper, never()).countByContestId(any());
        verify(snapshotService, never()).resetIfNoAttempts(any(), any());
    }

    @Test
    void missingActivityIsNotFoundAndInvalidTimeRangeIsBadRequest() {
        when(contestMapper.selectContestForUpdate(CONTEST_ID)).thenReturn(null);
        ContestDto request = new ContestDto();
        request.setContestId(CONTEST_ID);
        assertEquals(404, assertThrows(ApiStatusException.class, () -> service.update(request)).getStatusCode());

        when(contestMapper.selectContestForUpdate(CONTEST_ID)).thenReturn(current);
        request.setStartTime(NOW.plusHours(2));
        request.setEndTime(NOW.plusHours(1));
        assertEquals(400, assertThrows(ApiStatusException.class, () -> service.update(request)).getStatusCode());
    }

    @Test
    void participationMapperXmlParsesAndEveryMutationIsContestScoped() throws Exception {
        Configuration configuration = new MybatisConfiguration();
        parse(configuration, "/mapper/UserSubmitMapper.xml");
        parse(configuration, "/mapper/UserContestMapper.xml");
        parse(configuration, "/mapper/SupplementContestMapper.xml");
        parse(configuration, "/mapper/ContestAttemptMapper.xml");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("contestId", CONTEST_ID);
        parameters.put("userId", 812L);
        parameters.put("submitTime", NOW);
        parameters.put("deadline", NOW.plusHours(1));
        parameters.put("userIds", java.util.Arrays.asList(812L, 813L));
        parameters.put("relations", java.util.Arrays.asList(
                new com.anishan.problem.domain.entity.UserContestRelation(812L, CONTEST_ID),
                new com.anishan.problem.domain.entity.UserContestRelation(813L, CONTEST_ID)));

        String handIn = normalized(configuration.getMappedStatement(
                "com.anishan.problem.mapper.UserSubmitMapper.insertIgnore").getBoundSql(parameters).getSql());
        String deleteHandIn = normalized(configuration.getMappedStatement(
                "com.anishan.problem.mapper.UserSubmitMapper.deleteByContestAndUser")
                .getBoundSql(parameters).getSql());
        String addRoster = normalized(configuration.getMappedStatement(
                "com.anishan.problem.mapper.UserContestMapper.insertBatchIgnore").getBoundSql(parameters).getSql());
        String removeRoster = normalized(configuration.getMappedStatement(
                "com.anishan.problem.mapper.UserContestMapper.deleteByContestAndUsers")
                .getBoundSql(parameters).getSql());
        String supplement = normalized(configuration.getMappedStatement(
                "com.anishan.problem.mapper.SupplementContestMapper.upsertDeadline")
                .getBoundSql(parameters).getSql());
        String attempts = normalized(configuration.getMappedStatement(
                "com.anishan.problem.mapper.ContestAttemptMapper.selectUserIdsWithAttempts")
                .getBoundSql(parameters).getSql());

        assertTrue(handIn.contains("insert ignore into user_submit"));
        assertTrue(deleteHandIn.contains("where contest_id = ? and user_id = ?"));
        assertTrue(addRoster.contains("insert ignore into user_contest"));
        assertTrue(removeRoster.contains("where contest_id = ? and user_id in"));
        assertTrue(supplement.contains("on duplicate key update deadline = values(deadline)"));
        assertTrue(attempts.contains("where contest_id = ? and user_id in"));
    }

    private static void parse(Configuration configuration, String resource) throws Exception {
        try (InputStream input = ContestScheduleTest.class.getResourceAsStream(resource)) {
            assertNotNull(input, resource);
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    private static String normalized(String sql) {
        return sql.replaceAll("\\s+", " ").trim().toLowerCase(java.util.Locale.ROOT);
    }
}
