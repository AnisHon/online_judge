package com.anishan.content.service;

import com.anishan.content.mapper.ContentSolutionMigrationStateMapper;
import com.anishan.commons.exception.ApiStatusException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SolutionDomainMigrationGateTest {
    @Test
    void onlyTheExactCutoverMarkerEnablesSolutionWritesAndRefresh() {
        ContentSolutionMigrationStateMapper mapper = mock(ContentSolutionMigrationStateMapper.class);
        when(mapper.selectStatus("solution-domain-v1")).thenReturn("VERIFIED", "VERIFIED", "CUTOVER");
        SolutionDomainMigrationGate gate = new SolutionDomainMigrationGate(mapper);

        assertFalse(gate.isCutover());
        assertThrows(ApiStatusException.class, gate::requireCutover);
        assertTrue(gate.isCutover());
        verify(mapper, org.mockito.Mockito.times(3)).selectStatus("solution-domain-v1");
    }

    @Test
    void unreadableMigrationStateFailsClosedWithServiceUnavailable() {
        ContentSolutionMigrationStateMapper mapper = mock(ContentSolutionMigrationStateMapper.class);
        when(mapper.selectStatus("solution-domain-v1"))
                .thenThrow(new DataAccessResourceFailureException("table missing"));
        SolutionDomainMigrationGate gate = new SolutionDomainMigrationGate(mapper);

        ApiStatusException failure = assertThrows(ApiStatusException.class, gate::requireCutover);
        assertEquals(503, failure.getStatusCode());
        assertThrows(ApiStatusException.class, gate::isCutover);
    }
}
