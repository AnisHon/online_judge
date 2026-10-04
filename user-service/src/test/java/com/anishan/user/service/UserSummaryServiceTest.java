package com.anishan.user.service;

import com.anishan.api.client.user.domain.dto.UserSummaryRequest;
import com.anishan.api.client.user.domain.vo.UserSummaryVo;
import com.anishan.api.util.AuthUtil;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.user.domain.dto.UserSummaryRow;
import com.anishan.user.config.UserConfig;
import com.anishan.user.mapper.SysUserMapper;
import com.anishan.user.service.impl.SysUserServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.validation.Validation;
import javax.validation.Validator;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserSummaryServiceTest {

    private static final Long LARGE_ID = 2098765432109876543L;

    @Test
    void batchSummaryDeduplicatesIdsPreservesRequestedOrderAndOnlyReturnsSafeFields() throws Exception {
        SysUserMapper mapper = mock(SysUserMapper.class);
        SysUserServiceImpl service = service(mapper);
        when(mapper.selectUserSummaryRowsByIds(Arrays.asList(LARGE_ID, 12L))).thenReturn(Arrays.asList(
                row(LARGE_ID, "alice", "Alice", "管理员"),
                row(LARGE_ID, "alice", "Alice", "教师"),
                row(12L, "bob", "Bob", null),
                row(999L, "missing-race", "Not requested", null)
        ));

        List<UserSummaryVo> summaries = service.getUserSummariesByIds(Arrays.asList(LARGE_ID, 12L, LARGE_ID));

        assertEquals(Arrays.asList(LARGE_ID, 12L), Arrays.asList(
                summaries.get(0).getUserId(), summaries.get(1).getUserId()));
        assertEquals(Arrays.asList("管理员", "教师"), summaries.get(0).getSpecialRoles());
        assertEquals(Collections.emptyList(), summaries.get(1).getSpecialRoles());
        JsonNode json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(summaries.get(0)));
        Set<String> fields = new HashSet<>();
        json.fieldNames().forEachRemaining(fields::add);
        assertEquals(new HashSet<>(Arrays.asList("userId", "userName", "nikeName", "specialRoles")), fields);
        assertEquals(String.valueOf(LARGE_ID), json.get("userId").asText());
        verify(mapper).selectUserSummaryRowsByIds(Arrays.asList(LARGE_ID, 12L));
    }

    @Test
    void summaryServiceRejectsMoreThanOneHundredIds() {
        SysUserMapper mapper = mock(SysUserMapper.class);
        SysUserServiceImpl service = service(mapper);
        List<Long> ids = new ArrayList<>();
        for (long id = 1; id <= 101; id++) ids.add(id);

        assertEquals(400, assertThrows(ApiStatusException.class,
                () -> service.getUserSummariesByIds(ids)).getStatusCode());
        verifyNoInteractions(mapper);
    }

    @Test
    void internalSummaryRequestValidatesBatchLimitAndDecimalStringIds() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        UserSummaryRequest request = new UserSummaryRequest();
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 101; i++) ids.add(String.valueOf(i + 1));
        request.setUserIds(ids);
        assertFalse(validator.validate(request).isEmpty());

        request.setUserIds(Collections.singletonList("+17"));
        assertFalse(validator.validate(request).isEmpty());

        request.setUserIds(Collections.singletonList(String.valueOf(LARGE_ID)));
        assertTrue(validator.validate(request).isEmpty());
    }

    private static SysUserServiceImpl service(SysUserMapper mapper) {
        return new SysUserServiceImpl(mapper, mock(com.anishan.user.service.SysRoleService.class),
                mock(com.anishan.user.service.SysUserRoleService.class), mock(PasswordEncoder.class),
                mock(UserConfig.class), mock(com.anishan.user.service.SysMenuService.class), mock(AuthUtil.class),
                mock(UserConfig.class));
    }

    private static UserSummaryRow row(Long userId, String userName, String nikeName, String specialRole) {
        UserSummaryRow row = new UserSummaryRow();
        row.setUserId(userId);
        row.setUserName(userName);
        row.setNikeName(nikeName);
        row.setSpecialRole(specialRole);
        return row;
    }
}
