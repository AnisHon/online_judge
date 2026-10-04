package com.anishan.content.service.impl;

import com.anishan.api.client.problem.client.ProblemContentReadClient;
import com.anishan.api.client.problem.domain.dto.ContentProblemReadRequest;
import com.anishan.api.client.problem.domain.vo.ContentProblemReadVo;
import com.anishan.api.client.user.client.UserInternalClient;
import com.anishan.content.domain.enumeration.SolutionModerationState;
import com.anishan.content.domain.dto.PagedSolution;
import com.anishan.content.domain.vo.SolutionCandidate;
import com.anishan.content.domain.vo.SolutionCandidatePage;
import com.anishan.content.domain.vo.DetailSolutionVo;
import com.anishan.content.domain.vo.SolutionRecord;
import com.anishan.content.mapper.SolutionExplanationMapper;
import com.anishan.content.mapper.SolutionProblemReferenceMapper;
import com.anishan.content.service.SolutionAccessService;
import com.anishan.content.service.SolutionDomainMigrationGate;
import com.anishan.content.service.SolutionLocalWriteService;
import com.anishan.content.service.SolutionProblemReferenceService;
import com.anishan.content.service.SolutionProblemReferenceWriter;
import com.anishan.content.service.SolutionReferenceFenceConflictException;
import com.anishan.content.service.SolutionQueryService;
import com.anishan.commons.domain.R;
import com.anishan.commons.exception.ApiStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SolutionDomainReadTest {
    @Test
    void authorCanReadHistoricalPrivateBodyWhenAssociatedProblemNoLongerExists() {
        SolutionRecord solution = record(55L, 9007199254740993L, 42L, true);
        SolutionQueryService query = mock(SolutionQueryService.class);
        SolutionProblemReferenceService references = mock(SolutionProblemReferenceService.class);
        UserInternalClient users = mock(UserInternalClient.class);
        when(query.record(55L)).thenReturn(solution, solution);
        when(references.refresh(Collections.singletonList(solution.getProblemId())))
                .thenReturn(Collections.singletonMap(solution.getProblemId(), problem(solution.getProblemId(),
                        false, true, null, false, false, null)));
        when(users.nikeName(Collections.singletonList(42L)))
                .thenReturn(R.success(Collections.singletonMap(42L, "作者甲")));

        SolutionExplanationServiceImpl service = service(query, references, users);
        var detail = service.get(55L, 42L);

        assertEquals("9007199254740993", String.valueOf(detail.getProblemId()));
        assertEquals("关联题目不可用", detail.getProblemTitle());
        assertEquals("private solution body", detail.getContent());
        assertEquals("作者甲", detail.getNikeName());
    }

    @Test
    void privateAndContestSolutionsAreNotExposedToAnotherUser() {
        SolutionRecord solution = record(55L, 99L, 42L, true);
        SolutionQueryService query = mock(SolutionQueryService.class);
        SolutionProblemReferenceService references = mock(SolutionProblemReferenceService.class);
        when(query.record(55L)).thenReturn(solution);
        when(references.refresh(Collections.singletonList(99L))).thenReturn(Collections.singletonMap(99L,
                problem(99L, true, false, 2, false, false, null)));

        ApiStatusException failure = assertThrows(ApiStatusException.class,
                () -> service(query, references, mock(UserInternalClient.class)).get(55L, 77L));

        assertEquals(404, failure.getStatusCode());
    }

    @Test
    void publicSolutionIsDeniedImmediatelyWhenFreshProblemCheckNoLongerAllowsIt() {
        SolutionRecord solution = record(55L, 99L, 42L, false);
        SolutionQueryService query = mock(SolutionQueryService.class);
        SolutionProblemReferenceService references = mock(SolutionProblemReferenceService.class);
        when(query.record(55L)).thenReturn(solution, solution);
        when(references.refresh(Collections.singletonList(99L))).thenReturn(Collections.singletonMap(99L,
                problem(99L, true, false, 2, false, false, null)));

        ApiStatusException failure = assertThrows(ApiStatusException.class,
                () -> service(query, references, mock(UserInternalClient.class)).get(55L, 77L));

        assertEquals(404, failure.getStatusCode());
    }

    @Test
    void problemReadFailureFailsClosedInsteadOfUsingProjection() {
        ProblemContentReadClient client = mock(ProblemContentReadClient.class);
        when(client.readProblemContent(any(ContentProblemReadRequest.class))).thenThrow(new IllegalStateException("offline"));

        ApiStatusException failure = assertThrows(ApiStatusException.class,
                () -> new SolutionAccessService(client).readFresh(Collections.singletonList(99L)));

        assertEquals(503, failure.getStatusCode());
    }

    @Test
    void aStaleProjectionResponseIsDiscardedAndProblemStateIsFetchedAgain() {
        Long problemId = 99L;
        SolutionProblemReferenceMapper mapper = mock(SolutionProblemReferenceMapper.class);
        SolutionAccessService access = mock(SolutionAccessService.class);
        SolutionProblemReferenceWriter writer = mock(SolutionProblemReferenceWriter.class);
        when(mapper.selectByProblemIds(Collections.singletonList(problemId))).thenReturn(
                Collections.emptyList(), Collections.emptyList());
        ContentProblemReadVo stale = problem(problemId, true, false, 1, true, true, "old title");
        ContentProblemReadVo current = problem(problemId, true, false, 1, true, true, "current title");
        when(access.readFresh(Collections.singletonList(problemId))).thenReturn(
                Collections.singletonMap(problemId, stale), Collections.singletonMap(problemId, current));
        when(access.isPubliclyReadable(any(ContentProblemReadVo.class))).thenReturn(true);
        doThrow(new SolutionReferenceFenceConflictException()).doNothing()
                .when(writer).storeFenced(any(List.class), any());

        Map<Long, ContentProblemReadVo> result = new SolutionProblemReferenceService(mapper, access, writer)
                .refresh(Collections.singletonList(problemId));

        assertEquals("current title", result.get(problemId).getTitle());
        verify(access, times(2)).readFresh(Collections.singletonList(problemId));
        verify(writer, times(2)).storeFenced(any(List.class), any());
    }

    @Test
    void nonPublicTitlesAreNeverPersistedInTheProblemProjection() {
        Long problemId = 99L;
        SolutionProblemReferenceMapper mapper = mock(SolutionProblemReferenceMapper.class);
        SolutionAccessService access = mock(SolutionAccessService.class);
        SolutionProblemReferenceWriter writer = mock(SolutionProblemReferenceWriter.class);
        when(mapper.selectByProblemIds(Collections.singletonList(problemId))).thenReturn(Collections.emptyList());
        ContentProblemReadVo managed = problem(problemId, true, false, 2, false, true, "contest title");
        when(access.readFresh(Collections.singletonList(problemId))).thenReturn(Collections.singletonMap(problemId, managed));
        when(access.isPubliclyReadable(managed)).thenReturn(false);
        doNothing().when(writer).storeFenced(any(List.class), any());

        new SolutionProblemReferenceService(mapper, access, writer).refresh(Collections.singletonList(problemId));

        org.mockito.ArgumentCaptor<List> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(writer).storeFenced(captor.capture(), any());
        @SuppressWarnings("unchecked")
        List<com.anishan.content.domain.entity.SolutionProblemReference> stored = captor.getValue();
        assertEquals(1, stored.size());
        assertEquals(null, stored.get(0).getTitle());
    }

    @Test
    void listRepairsAtMostThreeFreshBatchesThenStopsSafely() {
        SolutionQueryService query = mock(SolutionQueryService.class);
        SolutionProblemReferenceService references = mock(SolutionProblemReferenceService.class);
        SolutionAccessService access = mock(SolutionAccessService.class);
        UserInternalClient users = mock(UserInternalClient.class);
        ContentProblemReadVo hiddenProblem = problem(99L, true, false, 2, false, false, null);
        when(references.refresh(Collections.singletonList(99L)))
                .thenReturn(Collections.singletonMap(99L, hiddenProblem));
        when(access.isPubliclyReadable(hiddenProblem)).thenReturn(false);
        List<SolutionCandidate> first = candidates(1L);
        List<SolutionCandidate> second = candidates(101L);
        List<SolutionCandidate> third = candidates(201L);
        when(query.firstPage(7L, false, false, null, null, 0L))
                .thenReturn(new SolutionCandidatePage(1000L, first));
        when(query.nextBatch(7L, false, false, null, null, first.get(99))).thenReturn(second);
        when(query.nextBatch(7L, false, false, null, null, second.get(99))).thenReturn(third);
        when(query.nextBatch(7L, false, false, null, null, third.get(99)))
                .thenReturn(Collections.emptyList());
        when(query.records(any(List.class))).thenAnswer(invocation -> {
            List<Long> ids = invocation.getArgument(0);
            return ids.stream().map(id -> record(id, 99L, 1000L, false)).collect(Collectors.toList());
        });
        PagedSolution page = new PagedSolution();
        page.setCurrentPage(1L);
        page.setPageSize(1L);

        var result = service(query, access, references, users).pagedQuery(7L, page);

        assertEquals(1000L, result.getTotalRecords());
        assertTrue(result.getData().isEmpty());
        verify(references, times(3)).refresh(Collections.singletonList(99L));
        verify(query, times(3)).nextBatch(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(false), org.mockito.ArgumentMatchers.eq(false),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), any(SolutionCandidate.class));
    }

    @Test
    void detailSerializesNineteenDigitIdsAsStrings() throws Exception {
        SolutionRecord solution = record(55L, 9007199254740993L, 42L, true);
        SolutionQueryService query = mock(SolutionQueryService.class);
        SolutionProblemReferenceService references = mock(SolutionProblemReferenceService.class);
        when(query.record(55L)).thenReturn(solution, solution);
        when(references.refresh(Collections.singletonList(solution.getProblemId())))
                .thenReturn(Collections.singletonMap(solution.getProblemId(), problem(solution.getProblemId(),
                        false, true, null, false, false, null)));
        DetailSolutionVo detail = service(query, references, mock(UserInternalClient.class)).get(55L, 42L);

        String json = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(detail);
        assertTrue(json.contains("\"problemId\":\"9007199254740993\""), json);
        assertFalse(json.contains("\"problemId\":9007199254740993"), json);
    }

    @Test
    void ordinaryCandidateSqlKeepsProblemFilterOutsideGroupedVisibilityPredicate() throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("mapper/SolutionExplanationMapper.xml")) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, "mapper/SolutionExplanationMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("viewerId", 77L);
        parameters.put("admin", false);
        parameters.put("publicOnly", false);
        parameters.put("filterUserId", null);
        parameters.put("problemId", 99L);
        parameters.put("offset", 0L);
        parameters.put("limit", 100);
        parameters.put("cursor", null);
        BoundSql boundSql = configuration.getMappedStatement(
                SolutionExplanationMapper.class.getName() + ".selectCandidates").getBoundSql(parameters);
        String sql = boundSql.getSql().toLowerCase().replaceAll("\\s+", "");

        assertTrue(sql.contains("ands.problem_id=?and(s.user_id=?or(s.`private`=0"), sql);
        assertTrue(sql.contains("s.moderation_state='normal'andr.exists_flag=1andr.del_flag=0andr.auth=1)"));
        assertTrue(sql.contains("orderbycoalesce(s.top_up,0)desc,s.create_timedesc,s.solution_iddesc"), sql);
    }

    private SolutionExplanationServiceImpl service(SolutionQueryService query,
                                                   SolutionProblemReferenceService references,
                                                   UserInternalClient users) {
        return service(query, mock(SolutionAccessService.class), references, users);
    }

    private SolutionExplanationServiceImpl service(SolutionQueryService query, SolutionAccessService access,
                                                   SolutionProblemReferenceService references,
                                                   UserInternalClient users) {
        return new SolutionExplanationServiceImpl(query, access, references,
                mock(SolutionDomainMigrationGate.class), mock(SolutionLocalWriteService.class),
                mock(com.anishan.content.service.SolutionLikeService.class), users);
    }

    private List<SolutionCandidate> candidates(long firstId) {
        List<SolutionCandidate> candidates = new ArrayList<>();
        for (long id = firstId; id < firstId + 100; id++) {
            SolutionCandidate candidate = new SolutionCandidate();
            candidate.setSolutionId(id);
            candidate.setProblemId(99L);
            candidate.setUserId(1000L);
            candidate.setVersion(3L);
            candidate.setTopUp(false);
            candidate.setCreateTime(java.time.LocalDateTime.of(2026, 1, 1, 0, 0).plusSeconds(id));
            candidates.add(candidate);
        }
        return candidates;
    }

    private SolutionRecord record(Long id, Long problemId, Long authorId, boolean privateValue) {
        SolutionRecord record = new SolutionRecord();
        record.setSolutionId(id);
        record.setProblemId(problemId);
        record.setUserId(authorId);
        record.setTitle("题解标题");
        record.setContent("private solution body");
        record.setPrivate_(privateValue);
        record.setDelFlag(false);
        record.setVersion(3L);
        record.setModerationState(SolutionModerationState.NORMAL);
        return record;
    }

    private ContentProblemReadVo problem(Long id, boolean exists, boolean deleted, Integer auth,
                                         boolean publicReadable, boolean privateWritable, String title) {
        return new ContentProblemReadVo(String.valueOf(id), exists, deleted, auth,
                publicReadable, privateWritable, title);
    }
}
