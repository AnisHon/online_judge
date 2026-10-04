package com.anishan.content.service;

import com.anishan.content.domain.vo.LikeResultVo;

import java.util.Collection;
import java.util.Set;

public interface SolutionLikeService {
    LikeResultVo like(Long userId, Long solutionId);

    LikeResultVo unlike(Long userId, Long solutionId);

    boolean isLiked(Long userId, Long solutionId);

    Set<Long> likedSolutionIds(Long userId, Collection<Long> solutionIds);
}
