package com.anishan.problem.service;

import com.anishan.problem.domain.dto.FinalRankPageQuery;
import com.anishan.problem.domain.vo.FinalRankVo;

public interface ContestRankQueryService {

    FinalRankVo getPublicRank(Long contestId, Long viewerId, FinalRankPageQuery query);

    FinalRankVo getAdminRank(Long contestId, FinalRankPageQuery query);

    boolean requestRebuild(Long contestId);
}
