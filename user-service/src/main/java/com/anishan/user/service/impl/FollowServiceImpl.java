package com.anishan.user.service.impl;

import com.anishan.api.client.user.domain.vo.UserSummaryVo;
import com.anishan.api.util.AuthUtil;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.exception.ApiStatusException;
import com.anishan.user.domain.dto.FollowPageQuery;
import com.anishan.user.domain.entity.UserFollow;
import com.anishan.user.domain.vo.FollowSummaryVo;
import com.anishan.user.mapper.UserFollowMapper;
import com.anishan.user.service.FollowService;
import com.anishan.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FollowServiceImpl implements FollowService {

    private final UserFollowMapper userFollowMapper;
    private final SysUserService sysUserService;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FollowSummaryVo follow(Long followeeId) {
        Long followerId = currentUserId();
        validateTargetId(followeeId);
        if (followerId <= 0) {
            throw new ApiStatusException(400, "系统账号不支持关注");
        }
        if (followerId.equals(followeeId)) {
            throw new ApiStatusException(400, "不能关注自己");
        }
        if (!sysUserService.isFollowableUser(followeeId)) {
            throw unavailableUser();
        }

        UserFollow follow = new UserFollow();
        follow.setFollowerId(followerId);
        follow.setFolloweeId(followeeId);
        follow.setCreatedAt(LocalDateTime.now());
        try {
            // The composite primary key is the concurrency guard; never pre-read before this insert.
            userFollowMapper.insert(follow);
        } catch (DuplicateKeyException duplicate) {
            // Concurrent/repeated PUT is idempotent. Other persistence failures must propagate.
        }
        return buildSummary(followerId, followeeId);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FollowSummaryVo unfollow(Long followeeId) {
        Long followerId = currentUserId();
        validateTargetId(followeeId);
        if (followerId <= 0) {
            throw new ApiStatusException(400, "系统账号不支持关注");
        }
        if (followerId.equals(followeeId)) {
            throw new ApiStatusException(400, "不能取消对自己的关注");
        }
        requireVisibleUser(followeeId);
        userFollowMapper.deleteByFollowerAndFollowee(followerId, followeeId);
        return buildSummary(followerId, followeeId);
    }

    @Override
    @Transactional(readOnly = true)
    public FollowSummaryVo getSummary(Long followeeId) {
        Long viewerId = currentUserId();
        validateTargetId(followeeId);
        requireVisibleUser(followeeId);
        return buildSummary(viewerId, followeeId);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<UserSummaryVo> getFollowing(Long userId, FollowPageQuery query) {
        currentUserId();
        requireVisibleUser(userId);
        PageBounds bounds = pageBounds(query);
        long total = userFollowMapper.countFollowing(userId);
        List<Long> ids = userFollowMapper.selectFollowingPage(userId, bounds.offset, bounds.pageSize);
        return pageResult(query, total, sysUserService.getUserSummariesByIds(ids));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<UserSummaryVo> getFollowers(Long userId, FollowPageQuery query) {
        currentUserId();
        requireVisibleUser(userId);
        PageBounds bounds = pageBounds(query);
        long total = userFollowMapper.countFollowers(userId);
        List<Long> ids = userFollowMapper.selectFollowersPage(userId, bounds.offset, bounds.pageSize);
        return pageResult(query, total, sysUserService.getUserSummariesByIds(ids));
    }

    private FollowSummaryVo buildSummary(Long viewerId, Long followeeId) {
        FollowSummaryVo summary = new FollowSummaryVo();
        summary.setUserId(followeeId);
        summary.setFollowing(viewerId != null && viewerId > 0
                && userFollowMapper.exists(viewerId, followeeId));
        summary.setFollowersCount(userFollowMapper.countFollowers(followeeId));
        summary.setFollowingCount(userFollowMapper.countFollowing(followeeId));
        return summary;
    }

    private PagedResult<UserSummaryVo> pageResult(FollowPageQuery query,
                                                  long total,
                                                  List<UserSummaryVo> data) {
        PagedResult<UserSummaryVo> result = new PagedResult<>();
        result.setCurrentPage(query.getCurrentPage());
        result.setPageSize(query.getPageSize());
        result.setTotalRecords(total);
        result.setData(data == null ? Collections.emptyList() : data);
        return result;
    }

    private PageBounds pageBounds(FollowPageQuery query) {
        if (query == null || query.getCurrentPage() == null || query.getPageSize() == null
                || query.getCurrentPage() < 1 || query.getPageSize() < 1 || query.getPageSize() > 50) {
            throw new ApiStatusException(400, "分页参数不合法");
        }
        try {
            long offset = Math.multiplyExact(query.getCurrentPage() - 1, query.getPageSize());
            return new PageBounds(offset, query.getPageSize().intValue());
        } catch (ArithmeticException overflow) {
            throw new ApiStatusException(400, "分页参数超出范围");
        }
    }

    private Long currentUserId() {
        Long userId = AuthUtil.getUserId();
        if (userId == null || userId < 0) {
            throw new ApiStatusException(401, "请先登录");
        }
        return userId;
    }

    private void validateTargetId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ApiStatusException(400, "用户ID不合法");
        }
    }

    private void requireVisibleUser(Long userId) {
        validateTargetId(userId);
        if (!sysUserService.isVisibleUser(userId)) {
            throw unavailableUser();
        }
    }

    private ApiStatusException unavailableUser() {
        return new ApiStatusException(404, "用户不存在或当前不可用");
    }

    private static final class PageBounds {
        private final long offset;
        private final int pageSize;

        private PageBounds(long offset, int pageSize) {
            this.offset = offset;
            this.pageSize = pageSize;
        }
    }
}
