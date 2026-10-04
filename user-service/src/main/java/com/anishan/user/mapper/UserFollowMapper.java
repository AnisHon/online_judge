package com.anishan.user.mapper;

import com.anishan.user.domain.entity.UserFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.time.LocalDateTime;

/** Explicit SQL for the composite-key follow relationship. */
@Mapper
public interface UserFollowMapper {

    int insert(@Param("follow") UserFollow follow);

    int deleteByFollowerAndFollowee(@Param("followerId") Long followerId,
                                    @Param("followeeId") Long followeeId);

    long countFollowing(@Param("followerId") Long followerId);

    long countFollowers(@Param("followeeId") Long followeeId);

    boolean exists(@Param("followerId") Long followerId,
                   @Param("followeeId") Long followeeId);

    List<Long> selectFollowingPage(@Param("followerId") Long followerId,
                                   @Param("offset") Long offset,
                                   @Param("pageSize") Integer pageSize);

    List<Long> selectFollowersPage(@Param("followeeId") Long followeeId,
                                   @Param("offset") Long offset,
                                   @Param("pageSize") Integer pageSize);

    List<Long> selectActiveFollowersForFanout(@Param("followeeId") Long followeeId,
                                             @Param("cursorUserId") Long cursorUserId,
                                             @Param("occurredAt") LocalDateTime occurredAt,
                                             @Param("limit") Integer limit);
}
