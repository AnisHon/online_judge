package com.anishan.user.service;

import com.anishan.api.client.user.domain.vo.UserSummaryVo;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.user.domain.dto.FollowPageQuery;
import com.anishan.user.domain.vo.FollowSummaryVo;

public interface FollowService {

    FollowSummaryVo follow(Long followeeId);

    FollowSummaryVo unfollow(Long followeeId);

    FollowSummaryVo getSummary(Long followeeId);

    PagedResult<UserSummaryVo> getFollowing(Long userId, FollowPageQuery query);

    PagedResult<UserSummaryVo> getFollowers(Long userId, FollowPageQuery query);
}
