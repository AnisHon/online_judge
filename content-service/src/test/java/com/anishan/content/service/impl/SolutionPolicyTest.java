package com.anishan.content.service.impl;

import com.anishan.api.client.user.client.UserInternalClient;
import com.anishan.api.domain.LoginUser;
import com.anishan.api.domain.entity.SysUser;
import com.anishan.api.util.AccountPolicy;
import com.anishan.content.domain.dto.DetailSolutionDto;
import com.anishan.content.domain.vo.SolutionRecord;
import com.anishan.api.client.problem.domain.vo.ContentProblemReadVo;
import com.anishan.content.service.SolutionDomainMigrationGate;
import com.anishan.content.service.SolutionExplanationService;
import com.anishan.content.service.SolutionLocalWriteService;
import com.anishan.content.service.SolutionProblemReferenceService;
import com.anishan.content.service.SolutionQueryService;
import com.anishan.api.client.problem.client.ProblemContentReadClient;
import com.anishan.content.service.SolutionAccessService;
import com.anishan.commons.exception.ApiStatusException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SolutionPolicyTest {
    private final SolutionQueryService query = mock(SolutionQueryService.class);
    private final SolutionProblemReferenceService references = mock(SolutionProblemReferenceService.class);
    private final SolutionDomainMigrationGate gate = mock(SolutionDomainMigrationGate.class);
    private final SolutionLocalWriteService writer = mock(SolutionLocalWriteService.class);
    private final UserInternalClient userClient = mock(UserInternalClient.class);
    private final SolutionExplanationService service = new SolutionExplanationServiceImpl(query,
            mock(SolutionAccessService.class), references, gate, writer,
            mock(com.anishan.content.service.SolutionLikeService.class), userClient);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void login(Long userId, String... permissions) {
        LoginUser user = new LoginUser();
        user.setUser(new SysUser().setUserId(userId));
        user.setAuths(List.of(permissions));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    void denyPolicyBlocksOrdinaryAndAdministrativeWritesBeforeAnyDependencyCall() {
        login(42L, AccountPolicy.SOLUTION_DENY, "problem:solution:add", "problem:solution:edit",
                "problem:solution:remove");
        DetailSolutionDto dto = new DetailSolutionDto();

        assertThrows(AccessDeniedException.class, () -> service.add(42L, dto));
        assertThrows(AccessDeniedException.class, () -> service.update(42L, dto));
        assertThrows(AccessDeniedException.class, () -> service.delete(List.of(9L), 42L));
        assertThrows(AccessDeniedException.class, () -> service.adminAdd(42L, dto));
        assertThrows(AccessDeniedException.class, () -> service.adminUpdate(dto));
        assertThrows(AccessDeniedException.class, () -> service.adminDelete(List.of(9L)));
        assertThrows(AccessDeniedException.class, () -> service.setTopUp(9L, true));
        verifyNoInteractions(query, references, gate, writer, userClient);
    }

    @Test
    void bothCreateEndpointsRejectClientSuppliedSolutionIds() {
        login(42L);
        doNothing().when(gate).requireCutover();
        DetailSolutionDto dto = new DetailSolutionDto().setSolutionId(9007199254740993L);

        ApiStatusException ordinary = assertThrows(ApiStatusException.class, () -> service.add(42L, dto));
        ApiStatusException admin = assertThrows(ApiStatusException.class, () -> service.adminAdd(42L, dto));
        assertEquals(400, ordinary.getStatusCode());
        assertEquals(400, admin.getStatusCode());
        verify(gate, org.mockito.Mockito.times(2)).requireCutover();
        verifyNoInteractions(references, writer);
    }

    @Test
    void ordinaryDeleteRemainsOwnerScopedAndDoesNotRequirePositiveGrant() {
        login(42L);
        doNothing().when(gate).requireCutover();
        when(writer.deleteOwned(List.of(9L), 42L)).thenReturn(true);

        org.junit.jupiter.api.Assertions.assertTrue(service.delete(List.of(9L), 42L));
        verify(writer).deleteOwned(List.of(9L), 42L);
        verify(query, never()).record(9L);
    }

    @Test
    void callerCannotUseAnotherUsersIdentityForMutations() {
        login(42L);
        assertThrows(AccessDeniedException.class, () -> service.add(43L, new DetailSolutionDto()));
        verifyNoInteractions(gate, references, writer);
    }

    @Test
    void authorMayEditAnOrphanedHistoricalSolutionOnlyWhileKeepingItPrivate() {
        login(42L);
        doNothing().when(gate).requireCutover();
        SolutionRecord current = new SolutionRecord();
        current.setSolutionId(9L);
        current.setProblemId(9007199254740993L);
        current.setUserId(42L);
        current.setPrivate_(true);
        current.setDelFlag(false);
        current.setVersion(4L);
        when(query.record(9L)).thenReturn(current);
        when(references.refresh(List.of(current.getProblemId()))).thenReturn(java.util.Collections.singletonMap(
                current.getProblemId(), new ContentProblemReadVo(String.valueOf(current.getProblemId()),
                        false, true, null, false, false, null)));
        DetailSolutionDto dto = new DetailSolutionDto().setSolutionId(9L)
                .setProblemId(current.getProblemId()).setTitle("更新后的历史题解")
                .setPrivate_(true).setContent("body");
        when(writer.update(42L, dto, current, false)).thenReturn(true);

        org.junit.jupiter.api.Assertions.assertTrue(service.update(42L, dto));
        verify(writer).update(42L, dto, current, false);
    }

    @Test
    void historicalSolutionCannotBePublishedWhenItsProblemIsMissingOrNonPublic() {
        login(42L);
        doNothing().when(gate).requireCutover();
        SolutionRecord current = new SolutionRecord();
        current.setSolutionId(9L);
        current.setProblemId(99L);
        current.setUserId(42L);
        current.setPrivate_(true);
        current.setDelFlag(false);
        current.setVersion(4L);
        when(query.record(9L)).thenReturn(current);
        when(references.refresh(List.of(99L))).thenReturn(java.util.Collections.singletonMap(99L,
                new ContentProblemReadVo("99", true, false, 2, false, false, null)));
        DetailSolutionDto dto = new DetailSolutionDto().setSolutionId(9L).setProblemId(99L)
                .setTitle("题解").setPrivate_(false).setContent("body");

        ApiStatusException failure = assertThrows(ApiStatusException.class, () -> service.update(42L, dto));

        assertEquals(400, failure.getStatusCode());
        verify(writer, never()).update(42L, dto, current, false);
    }

    private SolutionExplanationServiceImpl service(SolutionAccessService access) {
        return new SolutionExplanationServiceImpl(query, access, references, gate, writer,
                mock(com.anishan.content.service.SolutionLikeService.class), userClient);
    }
}
