package com.anishan.content.service.impl;

import com.anishan.api.client.user.client.UserInternalClient;
import com.anishan.content.controller.SolutionController;
import com.anishan.content.domain.dto.PagedSolution;
import com.anishan.content.domain.dto.DetailSolutionDto;
import com.anishan.content.domain.enumeration.SolutionModerationState;
import com.anishan.content.domain.vo.SolutionCandidatePage;
import com.anishan.content.domain.vo.SolutionRecord;
import com.anishan.content.service.*;
import com.anishan.commons.exception.ApiStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.cache.annotation.Cacheable;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.anishan.content.service.impl.SolutionTestFixtures.*;

class SolutionVisibilityTest {
    final SolutionQueryService query = mock(SolutionQueryService.class);
    final SolutionProblemReferenceService references = mock(SolutionProblemReferenceService.class);
    final SolutionAccessService access = new SolutionAccessService(null);
    final SolutionLocalWriteService writer = mock(SolutionLocalWriteService.class);
    final SolutionExplanationServiceImpl service = new SolutionExplanationServiceImpl(query, access, references,
            mock(SolutionDomainMigrationGate.class), writer, mock(com.anishan.content.service.SolutionLikeService.class),
            mock(UserInternalClient.class));

    @BeforeEach void setUp() {
        login(AUTHOR, "problem:solution:list", "problem:solution:add");
        when(query.record(ID)).thenReturn(row());
        when(references.refresh(List.of(77L))).thenReturn(Map.of(77L, problem(true)));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void privateAuthorReadCannotWarmAResponseForOtherReadersEvenWithAdminPermission() throws Exception {
        SolutionRecord row = row(); row.setPrivate_(true);
        when(query.record(ID)).thenReturn(row);
        assertEquals("AUTHOR_ONLY", service.get(ID, AUTHOR).getEffectiveVisibility());
        assertEquals(404, assertThrows(ApiStatusException.class, () -> service.get(ID, 99L)).getStatusCode());
        assertEquals("AUTHOR_ONLY", service.adminGet(ID).getEffectiveVisibility());
        assertNull(SolutionExplanationServiceImpl.class.getMethod("get", Long.class, Long.class).getAnnotation(Cacheable.class));
        assertNull(SolutionExplanationServiceImpl.class.getMethod("recent").getAnnotation(Cacheable.class));
        verify(references, times(3)).refresh(List.of(77L));
    }

    @Test void everyReadRechecksRestrictionAndLiveProblemState() {
        SolutionRecord row = row();
        when(query.record(ID)).thenReturn(row);
        assertEquals("PUBLIC", service.get(ID, 99L).getEffectiveVisibility());
        row.setModerationState(SolutionModerationState.AUTHOR_ONLY); row.setVersion(4L);
        assertEquals(404, assertThrows(ApiStatusException.class, () -> service.get(ID, 99L)).getStatusCode());
        assertEquals("AUTHOR_ONLY", service.get(ID, AUTHOR).getEffectiveVisibility());
        row.setModerationState(SolutionModerationState.NORMAL);
        when(references.refresh(anyList())).thenReturn(Map.of(77L, problem(false)));
        assertEquals(404, assertThrows(ApiStatusException.class, () -> service.get(ID, 99L)).getStatusCode());
        assertEquals("关联题目不可用", service.get(ID, AUTHOR).getProblemTitle());
    }

    @Test void detailKeepsInteractionFieldsAndLongIdsButNeverIncludesManagementReasons() throws Exception {
        var result = service.get(ID, 99L);
        assertEquals(2L, result.getLikeCount()); assertEquals(1L, result.getCommentCount());
        assertTrue(result.getCommentsOpen()); assertFalse(result.isLikedByMe());
        String json = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(result);
        assertTrue(json.contains("\"solutionId\":\"" + ID + "\""));
        assertFalse(json.contains("reason")); assertFalse(json.contains("operatorId"));
    }

    @Test void filterUserRemainsOutsideVisibilityWithoutMutatingCallerQuery() {
        PagedSolution page = new PagedSolution(); page.setUserId(99L); page.setProblemId(77L);
        page.setPageSize(20L); page.setCurrentPage(1L);
        when(query.firstPage(AUTHOR, false, false, 99L, 77L, 0L))
                .thenReturn(new SolutionCandidatePage(0, List.of()));
        assertTrue(service.pagedQuery(AUTHOR, page).getData().isEmpty());
        assertEquals(99L, page.getUserId());
        verify(query).firstPage(AUTHOR, false, false, 99L, 77L, 0L);
    }

    @Test void createAndAdminCreateRejectClientSuppliedIdBeforeAnyWrite() {
        DetailSolutionDto dto = dto(false).setSolutionId(ID);
        assertEquals(400, assertThrows(ApiStatusException.class, () -> service.add(AUTHOR, dto)).getStatusCode());
        assertEquals(400, assertThrows(ApiStatusException.class, () -> service.adminAdd(AUTHOR, dto)).getStatusCode());
        verifyNoInteractions(writer);
    }

    @Test void nonPublicCannotBePublishedByOrdinaryOrAdminCreationOrEditing() {
        when(references.refresh(anyList())).thenReturn(Map.of(77L, problem(false)));
        assertEquals(400, assertThrows(ApiStatusException.class, () -> service.add(AUTHOR, dto(false))).getStatusCode());
        assertEquals(400, assertThrows(ApiStatusException.class, () -> service.adminAdd(AUTHOR, dto(false))).getStatusCode());
        assertEquals(400, assertThrows(ApiStatusException.class, () -> service.update(AUTHOR, dto(false).setSolutionId(ID))).getStatusCode());
        assertEquals(400, assertThrows(ApiStatusException.class, () -> service.adminUpdate(dto(false).setSolutionId(ID))).getStatusCode());
        verifyNoInteractions(writer);
    }

    @Test void restrictedHistoricalAuthorCanEditPrivatelyButCannotChangeProblemOrAuthor() {
        SolutionRecord row = row(); row.setModerationState(SolutionModerationState.AUTHOR_ONLY);
        when(query.record(ID)).thenReturn(row);
        when(references.refresh(anyList())).thenReturn(Map.of(77L, problem(false)));
        DetailSolutionDto dto = dto(true).setSolutionId(ID);
        when(writer.update(AUTHOR, dto, row, false)).thenReturn(true);
        assertTrue(service.update(AUTHOR, dto));
        assertEquals(SolutionModerationState.AUTHOR_ONLY, row.getModerationState());
        dto.setProblemId(88L);
        assertEquals(400, assertThrows(ApiStatusException.class, () -> service.update(AUTHOR, dto)).getStatusCode());
        login(99L);
        assertEquals(404, assertThrows(ApiStatusException.class, () -> service.update(99L, dto)).getStatusCode());
    }

    @Test void remoteFailureFailsClosedAndDeletedSolutionsHaveNoOrdinaryDetail() {
        when(references.refresh(anyList())).thenThrow(new ApiStatusException(503, "安全错误"));
        assertEquals(503, assertThrows(ApiStatusException.class, () -> service.get(ID, AUTHOR)).getStatusCode());
        SolutionRecord deleted = row(); deleted.setDelFlag(true);
        when(query.record(ID)).thenReturn(deleted);
        assertEquals(404, assertThrows(ApiStatusException.class, () -> service.get(ID, AUTHOR)).getStatusCode());
    }

    @Test void oldAdminDeleteWithoutReasonRemains400AndControllerDoesNotTrustUserHeader() {
        assertEquals(400, assertThrows(ApiStatusException.class, () -> service.adminDelete(List.of(ID))).getStatusCode());
        SolutionExplanationService mocked = mock(SolutionExplanationService.class);
        SolutionController controller = new SolutionController(mocked, mock(SolutionModerationService.class),
                mock(com.anishan.content.service.SolutionLikeService.class),
                mock(com.anishan.content.service.CommentService.class),
                mock(com.anishan.content.service.CommentModerationService.class));
        controller.get(ID);
        verify(mocked).get(ID, AUTHOR);
    }

    @Test void controllerMapsBindingErrorsToMatchingHttpAndBodyWithoutInternalMessages() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                new SolutionController(service, mock(SolutionModerationService.class),
                        mock(com.anishan.content.service.SolutionLikeService.class),
                        mock(com.anishan.content.service.CommentService.class),
                        mock(com.anishan.content.service.CommentModerationService.class)))
                .setControllerAdvice(new com.anishan.api.advice.GlobalExceptionAdvice(
                        mock(com.anishan.commons.config.SharedConfig.class))).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/solution/admin/" + ID + "/moderation")
                .contentType("application/json").content("{\"action\":\"invalid\",\"reason\":\"reason\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(400));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/solution/not-a-number"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").value("请求参数有误"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/solution/admin/" + ID))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(400));
    }

    @Test void mapperKeepsFiltersOutsideVisibilityAndOwnHistoryContainsOnlySafeColumns() throws Exception {
        org.apache.ibatis.session.Configuration config = new org.apache.ibatis.session.Configuration();
        for (String name : List.of("SolutionExplanationMapper", "SolutionModerationActionMapper")) {
            try (var stream = getClass().getResourceAsStream("/mapper/" + name + ".xml")) {
                new org.apache.ibatis.builder.xml.XMLMapperBuilder(stream, config, name, config.getSqlFragments()).parse();
            }
        }
        Map<String, Object> params = new HashMap<>();
        params.put("filterUserId", 99L); params.put("problemId", 77L);
        params.put("viewerId", AUTHOR); params.put("publicOnly", false); params.put("admin", false);
        String sql = config.getMappedStatement("com.anishan.content.mapper.SolutionExplanationMapper.countCandidates")
                .getBoundSql(params).getSql().replaceAll("\\s+", " ").toLowerCase();
        assertTrue(sql.indexOf("s.user_id = ?") < sql.indexOf("and ("));
        assertTrue(sql.indexOf("s.problem_id = ?") < sql.indexOf("and ("));
        String own = config.getMappedStatement("com.anishan.content.mapper.SolutionModerationActionMapper.selectOwnSolutionActions")
                .getBoundSql(Map.of("solutionId", ID, "authorId", AUTHOR)).getSql().toLowerCase();
        assertTrue(own.contains("target_type = 'solution'")); assertTrue(own.contains("author_id ="));
        assertFalse(own.contains("operator_id")); assertTrue(own.contains("limit 50"));
        var bound = config.getMappedStatement("com.anishan.content.mapper.SolutionExplanationMapper.moderateByVersion")
                .getBoundSql(Map.of("solutionId", ID, "version", 3L, "state", "NORMAL", "deleted", false,
                        "firstPublishedAt", java.time.LocalDateTime.now(CLOCK), "now", java.time.LocalDateTime.now(CLOCK)));
        assertTrue(bound.getSql().contains("first_published_at = ?"));
        assertTrue(bound.getSql().contains("version = version + 1"));
        assertTrue(bound.getSql().contains("del_flag = 0"));
    }

    @Test void recentDoesNotReusePreviouslyPublicResponseAfterRestriction() {
        SolutionRecord row = row();
        com.anishan.content.domain.vo.SolutionCandidate candidate = new com.anishan.content.domain.vo.SolutionCandidate();
        candidate.setSolutionId(ID); candidate.setProblemId(77L); candidate.setVersion(3L);
        candidate.setUserId(AUTHOR); candidate.setCreateTime(row.getCreateTime());
        when(query.firstPage(null, false, true, null, null, 0L))
                .thenReturn(new SolutionCandidatePage(1, List.of(candidate)));
        when(query.records(List.of(ID))).thenReturn(List.of(row));
        assertEquals("PUBLIC", service.recent().get(0).getEffectiveVisibility());
        row.setModerationState(SolutionModerationState.AUTHOR_ONLY);
        assertTrue(service.recent().isEmpty());
        verify(references, times(2)).refresh(List.of(77L));
    }
}
