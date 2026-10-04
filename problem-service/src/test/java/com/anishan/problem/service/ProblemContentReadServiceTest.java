package com.anishan.problem.service;

import com.anishan.api.client.problem.domain.dto.ContentProblemReadRequest;
import com.anishan.api.client.problem.domain.vo.ContentProblemReadVo;
import com.anishan.api.domain.LoginUser;
import com.anishan.api.domain.entity.SysUser;
import com.anishan.commons.enumeration.ProblemAuth;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.problem.domain.entity.Problem;
import com.anishan.problem.mapper.ProblemMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProblemContentReadServiceTest {
    private final ProblemMapper mapper = mock(ProblemMapper.class);
    private final ProblemContentReadService service = new ProblemContentReadService(mapper);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ordinaryUserCanReadPublicMetadataButCannotReadContestMetadataOrWriteThere() {
        login(41L);
        when(mapper.selectContentReadByProblemIds(Arrays.asList(11L, 12L, 13L, 14L))).thenReturn(Arrays.asList(
                problem(11L, ProblemAuth.PUBLIC, 0, "public"),
                problem(12L, ProblemAuth.CONTEST, 0, "contest"),
                problem(13L, ProblemAuth.PUBLIC, 1, "deleted")
        ));

        List<ContentProblemReadVo> result = service.read(request("11", "12", "13", "14"));

        assertEquals(4, result.size());
        assertEquals("11", result.get(0).getProblemId());
        assertTrue(result.get(0).isPublicReadable());
        assertTrue(result.get(0).isPrivateWritable());
        assertEquals("public", result.get(0).getTitle());

        assertFalse(result.get(1).isPublicReadable());
        assertFalse(result.get(1).isPrivateWritable());
        assertNull(result.get(1).getTitle());

        assertTrue(result.get(2).isDeleted());
        assertFalse(result.get(2).isPublicReadable());
        assertFalse(result.get(2).isPrivateWritable());
        assertNull(result.get(2).getTitle());

        ContentProblemReadVo missing = result.get(3);
        assertFalse(missing.isExists());
        assertTrue(missing.isDeleted());
        assertNull(missing.getAuth());
        assertFalse(missing.isPublicReadable());
        assertFalse(missing.isPrivateWritable());
        assertNull(missing.getTitle());
    }

    @Test
    void exactProblemListPermissionAllowsPrivateContestWriteAndTitleOnly() {
        login(42L, "problem:problem:list");
        when(mapper.selectContentReadByProblemIds(Arrays.asList(20L, 21L))).thenReturn(Arrays.asList(
                problem(20L, ProblemAuth.CONTEST, 0, "contest title"),
                problem(21L, ProblemAuth.CONTEST, 1, "deleted title")
        ));

        List<ContentProblemReadVo> result = service.read(request("20", "21"));

        assertEquals(Integer.valueOf(2), result.get(0).getAuth());
        assertFalse(result.get(0).isPublicReadable());
        assertTrue(result.get(0).isPrivateWritable());
        assertEquals("contest title", result.get(0).getTitle());
        assertTrue(result.get(1).isDeleted());
        assertFalse(result.get(1).isPrivateWritable());
        assertEquals("deleted title", result.get(1).getTitle());
    }

    @Test
    void anonymousAndVirtualRootCallersReceiveOnlyPublicMetadata() {
        when(mapper.selectContentReadByProblemIds(Arrays.asList(30L, 31L))).thenReturn(Arrays.asList(
                problem(30L, ProblemAuth.PUBLIC, 0, "public"),
                problem(31L, ProblemAuth.CONTEST, 0, "contest")
        ));

        List<ContentProblemReadVo> anonymous = service.read(request("30", "31"));
        assertTrue(anonymous.get(0).isPublicReadable());
        assertFalse(anonymous.get(0).isPrivateWritable());
        assertEquals("public", anonymous.get(0).getTitle());
        assertNull(anonymous.get(1).getTitle());
        assertFalse(anonymous.get(1).isPrivateWritable());

        login(0L, "problem:problem:list");
        List<ContentProblemReadVo> virtualRoot = service.read(request("30", "31"));
        assertFalse(virtualRoot.get(0).isPrivateWritable());
        assertNull(virtualRoot.get(1).getTitle());
        assertFalse(virtualRoot.get(1).isPrivateWritable());
    }

    @Test
    void idsAreDeduplicatedWithoutLosingOrderOrPrecision() {
        long beyondJavaScriptSafeInteger = 9007199254740993L;
        login(43L);
        when(mapper.selectContentReadByProblemIds(Arrays.asList(beyondJavaScriptSafeInteger, 7L))).thenReturn(Arrays.asList(
                problem(7L, ProblemAuth.PUBLIC, 0, "seven"),
                problem(beyondJavaScriptSafeInteger, ProblemAuth.PUBLIC, 0, "large")
        ));

        List<ContentProblemReadVo> result = service.read(request(
                "9007199254740993", "7", "07", "9007199254740993"));

        assertEquals(2, result.size());
        assertEquals("9007199254740993", result.get(0).getProblemId());
        assertEquals("large", result.get(0).getTitle());
        assertEquals("7", result.get(1).getProblemId());
        verify(mapper).selectContentReadByProblemIds(Arrays.asList(beyondJavaScriptSafeInteger, 7L));
    }

    @Test
    void acceptsOneHundredIdsAndRejectsInvalidOrMoreThanOneHundred() {
        List<String> oneHundred = new ArrayList<>();
        for (int i = 1; i <= 100; i++) oneHundred.add(String.valueOf(i));
        when(mapper.selectContentReadByProblemIds(anyList())).thenReturn(Collections.emptyList());

        assertEquals(100, service.read(request(oneHundred.toArray(new String[0]))).size());
        assertThrows(IllegalArgumentException.class, () -> service.read(request(repeatIds(101))));
        assertThrows(IllegalArgumentException.class, () -> service.read(request("0")));
        assertThrows(IllegalArgumentException.class, () -> service.read(request("-1")));
        assertEquals("1", service.read(request("01")).get(0).getProblemId());
        assertThrows(IllegalArgumentException.class, () -> service.read(request("9223372036854775808")));
        assertThrows(IllegalArgumentException.class, () -> service.read(request("1.0")));
        assertThrows(IllegalArgumentException.class, () -> service.read(request()));
    }

    @Test
    void rejectsCallerSuppliedIdentityBeforeAnyProblemLookup() throws Exception {
        ContentProblemReadRequest forged = new ObjectMapper().readValue(
                "{\"problemIds\":[\"1\"],\"userId\":\"99\"}", ContentProblemReadRequest.class);

        ApiStatusException exception = assertThrows(ApiStatusException.class, () -> service.read(forged));

        assertEquals(400, exception.getStatusCode());
        verifyNoInteractions(mapper);
    }

    private void login(Long userId, String... permissions) {
        LoginUser user = new LoginUser();
        user.setUser(new SysUser());
        user.getUser().setUserId(userId);
        user.setAuths(Arrays.asList(permissions));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private Problem problem(Long id, ProblemAuth auth, Integer delFlag, String title) {
        Problem problem = new Problem();
        problem.setProblemId(id);
        problem.setAuth(auth);
        problem.setDelFlag(delFlag);
        problem.setTitle(title);
        return problem;
    }

    private ContentProblemReadRequest request(String... ids) {
        ContentProblemReadRequest request = new ContentProblemReadRequest();
        request.setProblemIds(Arrays.asList(ids));
        return request;
    }

    private String[] repeatIds(int count) {
        String[] ids = new String[count];
        for (int i = 0; i < count; i++) ids[i] = String.valueOf(i + 1);
        return ids;
    }
}
