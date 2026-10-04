package com.anishan.content.service.impl;

import com.anishan.content.mapper.SolutionExplanationContentMapper;
import com.anishan.content.service.SolutionExplanationContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SolutionExplanationContentServiceImpl implements SolutionExplanationContentService {
    private final SolutionExplanationContentMapper contentMapper;

    @Override
    public String findBySolutionId(Long solutionId) {
        return contentMapper.selectContentBySolutionId(solutionId);
    }

    @Override
    public void save(Long solutionId, String content) {
        contentMapper.upsertContent(solutionId, content);
    }
}
