package com.anishan.content.service;

public interface SolutionExplanationContentService {
    String findBySolutionId(Long solutionId);
    void save(Long solutionId, String content);
}
