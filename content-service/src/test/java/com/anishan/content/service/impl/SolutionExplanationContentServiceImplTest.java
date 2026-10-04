package com.anishan.content.service.impl;

import com.anishan.content.mapper.SolutionExplanationContentMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SolutionExplanationContentServiceImplTest {
    @Test
    void delegatesReadsAndWritesToTheContentDatabaseMapper() {
        SolutionExplanationContentMapper mapper = mock(SolutionExplanationContentMapper.class);
        when(mapper.selectContentBySolutionId(77L)).thenReturn("solution body");
        SolutionExplanationContentServiceImpl service = new SolutionExplanationContentServiceImpl(mapper);

        assertEquals("solution body", service.findBySolutionId(77L));
        service.save(77L, "updated body");

        verify(mapper).selectContentBySolutionId(77L);
        verify(mapper).upsertContent(77L, "updated body");
    }
}
