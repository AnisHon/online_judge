package com.anishan.content.service;

import com.anishan.api.client.problem.domain.vo.ContentProblemReadVo;
import com.anishan.content.domain.enumeration.SolutionModerationState;
import com.anishan.content.domain.vo.*;
import com.anishan.content.mapper.SolutionExplanationMapper;
import com.anishan.commons.exception.ApiStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import com.anishan.api.domain.LoginUser;
import com.anishan.api.domain.entity.SysUser;
import com.anishan.api.advice.GlobalExceptionAdvice;
import com.anishan.commons.config.SharedConfig;
import com.anishan.content.controller.SolutionProfileController;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SolutionProfilePrivacyTest {
    private final SolutionQueryService query = mock(SolutionQueryService.class);
    private final SolutionProblemReferenceService references = mock(SolutionProblemReferenceService.class);
    private final SolutionAccessService access = new SolutionAccessService(null);
    private final SolutionProfileQueryService service = new SolutionProfileQueryService(query, references, access);
    private static final long TARGET = 2098755579163770881L;

    @AfterEach
    void clearIdentity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void controllerUsesAuthenticatedIdentityNotAdminPermissionOrQueryFlag() throws Exception {
        LoginUser login = new LoginUser();
        SysUser user = new SysUser();
        user.setUserId(99L);
        login.setUser(user);
        login.setAuths(List.of("problem:solution:list"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(login, null, login.getAuthorities()));
        prepare(List.of(row(1L, true)));
        var mvc = MockMvcBuilders.standaloneSetup(new SolutionProfileController(service))
                .setControllerAdvice(new GlobalExceptionAdvice(mock(SharedConfig.class))).build();
        mvc.perform(get("/profile/" + TARGET + "/solutions"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(String.valueOf(TARGET)))
                .andExpect(jsonPath("$.data.solutions").isEmpty());
        mvc.perform(get("/profile/" + TARGET + "/solutions").param("includePrivate", "true"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        when(references.refresh(anyList())).thenThrow(new ApiStatusException(503, "题目状态暂不可用"));
        mvc.perform(get("/profile/" + TARGET + "/solutions"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(503));
    }

    @Test
    void cacheCatalogPreservesStableIdsAndRegistersOnlyCurrentProfileCache() {
        var byId = CacheCatalog.types().stream().collect(Collectors.toMap(
                com.anishan.content.domain.vo.CacheTypeVo::getId, item -> item));
        assertEquals("problem:profile:v3:", byId.get(23).getType());
        assertTrue(byId.get(9).getDesc().contains("停用"));
        assertTrue(byId.get(12).getDesc().contains("停用"));
        assertTrue(byId.get(21).getDesc().contains("停用"));
        assertEquals("content:comment:write:", byId.get(24).getType());
        assertEquals("problem:contest:final-rank:v1:", byId.get(25).getType());
    }

    @Test
    void onlyOwnerGetsPrivateRestrictedAndOrphanHistory() throws Exception {
        SolutionRecord publicRow = row(1L, false);
        SolutionRecord privateRow = row(2L, true);
        SolutionRecord restricted = row(3L, false);
        restricted.setModerationState(SolutionModerationState.AUTHOR_ONLY);
        List<SolutionRecord> rows = List.of(publicRow, privateRow, restricted);
        prepare(rows);
        var own = service.getSolutions(TARGET, TARGET);
        assertEquals(3, own.getSolutions().size());
        assertEquals("AUTHOR_ONLY", own.getSolutions().get(2).getEffectiveVisibility());
        assertEquals(1, service.getSolutions(TARGET, 99L).getSolutions().size());
        assertEquals(1, service.getSolutions(TARGET, null).getSolutions().size());
        // No admin flag exists in this ordinary profile API: another identity never gains owner scope.
        verify(query).profileBatch(TARGET, 99L, null);
        var missing = problem(false);
        missing.setTitle(null);
        when(references.refresh(anyList())).thenReturn(Map.of(42L, missing));
        var orphan = service.getSolutions(TARGET, TARGET);
        assertEquals("关联题目不可用", orphan.getSolutions().get(0).getProblemTitle());
        assertTrue(service.getSolutions(TARGET, 99L).getSolutions().isEmpty());
        String json = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(orphan);
        assertTrue(json.contains("\"userId\":\"" + TARGET + "\""));
        assertTrue(json.contains("\"solutionId\":\"1\""));
        assertTrue(json.contains("\"problemId\":\"42\""));
    }

    @Test
    void freshlyHiddenContentIsExcludedOnEveryCall() {
        SolutionRecord row = row(1L, false);
        prepare(List.of(row));
        assertEquals(1, service.getSolutions(TARGET, 99L).getSolutions().size());
        row.setModerationState(SolutionModerationState.AUTHOR_ONLY);
        assertTrue(service.getSolutions(TARGET, 99L).getSolutions().isEmpty());
        verify(references, times(2)).refresh(List.of(42L));
    }

    @Test
    void changedVersionOrAuthorAndDeletedRowsAreNotReturned() {
        SolutionRecord row = row(1L, false);
        prepare(List.of(row));
        row.setVersion(2L);
        assertTrue(service.getSolutions(TARGET, TARGET).getSolutions().isEmpty());
        row.setVersion(0L);
        row.setUserId(88L);
        assertTrue(service.getSolutions(TARGET, TARGET).getSolutions().isEmpty());
        row.setUserId(TARGET);
        row.setDelFlag(true);
        assertTrue(service.getSolutions(TARGET, TARGET).getSolutions().isEmpty());
    }

    @Test
    void fiftyOneValidEntriesAreTruncatedAndFiftyAreNot() {
        List<SolutionRecord> rows = IntStream.rangeClosed(1, 51).mapToObj(i -> row((long) i, false))
                .collect(Collectors.toList());
        prepare(rows);
        var result = service.getSolutions(TARGET, TARGET);
        assertEquals(50, result.getSolutions().size());
        assertTrue(result.isSolutionsTruncated());
        prepare(rows.subList(0, 50));
        assertFalse(service.getSolutions(TARGET, TARGET).isSolutionsTruncated());
    }

    @Test
    void repairRoundsAreBoundedAndRemoteFailureDoesNotBecomeAnEmptyList() {
        List<SolutionRecord> rows = IntStream.rangeClosed(1, 51).mapToObj(i -> row((long) i, true))
                .collect(Collectors.toList());
        prepare(rows);
        assertThrows(ApiStatusException.class, () -> service.getSolutions(TARGET, 99L));
        verify(references, times(3)).refresh(anyList());
        when(references.refresh(anyList())).thenThrow(new ApiStatusException(503, "题目状态暂不可用"));
        assertThrows(ApiStatusException.class, () -> service.getSolutions(TARGET, TARGET));
    }

    @Test
    void profileQueryKeepsTargetFilterOutsideVisibilityOrAndUses51Limit() throws Exception {
        SolutionExplanationMapper mapper = mock(SolutionExplanationMapper.class);
        new SolutionQueryService(mapper).profileBatch(TARGET, TARGET, null);
        verify(mapper).selectCandidates(TARGET, false, false, TARGET, null, 0L, 51, null);
        Configuration configuration = new Configuration();
        try (var input = getClass().getResourceAsStream("/mapper/SolutionExplanationMapper.xml")) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, "mapper/SolutionExplanationMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        Map<String, Object> args = new HashMap<>();
        args.put("viewerId", TARGET); args.put("filterUserId", TARGET);
        args.put("admin", false); args.put("publicOnly", false);
        args.put("limit", 51); args.put("offset", 0);
        String sql = configuration.getMappedStatement(SolutionExplanationMapper.class.getName() + ".selectCandidates")
                .getBoundSql(args).getSql().replaceAll("\\s+", " ");
        assertTrue(sql.contains("AND s.user_id = ? AND ("));
        assertFalse(sql.contains("FOR UPDATE"));
        assertTrue(sql.contains("LIMIT ?, ?"));
    }

    private void prepare(List<SolutionRecord> rows) {
        List<SolutionCandidate> candidates = rows.stream().map(row -> {
            SolutionCandidate item = new SolutionCandidate();
            item.setSolutionId(row.getSolutionId()); item.setProblemId(row.getProblemId());
            item.setUserId(row.getUserId()); item.setVersion(row.getVersion());
            return item;
        }).collect(Collectors.toList());
        when(query.profileBatch(eq(TARGET), any(), any())).thenReturn(candidates);
        when(query.profileRecords(anyList())).thenReturn(rows);
        when(references.refresh(anyList())).thenReturn(Map.of(42L, problem(true)));
    }

    private SolutionRecord row(Long id, boolean privateValue) {
        SolutionRecord row = new SolutionRecord();
        row.setSolutionId(id); row.setProblemId(42L); row.setUserId(TARGET);
        row.setVersion(0L); row.setTitle("题解"); row.setPrivate_(privateValue);
        row.setModerationState(SolutionModerationState.NORMAL); row.setDelFlag(false);
        return row;
    }

    private ContentProblemReadVo problem(boolean available) {
        return new ContentProblemReadVo("42", available, !available, available ? 1 : null,
                available, false, available ? "公开题目" : null);
    }
}
