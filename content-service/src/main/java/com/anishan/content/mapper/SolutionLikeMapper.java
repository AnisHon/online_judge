package com.anishan.content.mapper;

import com.anishan.content.domain.entity.SolutionLike;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SolutionLikeMapper {
    SolutionLike selectBySolutionAndUser(@Param("solutionId") Long solutionId, @Param("userId") Long userId);
    SolutionLike selectBySolutionAndUserForUpdate(@Param("solutionId") Long solutionId,
                                                   @Param("userId") Long userId);
    int insertState(@Param("record") SolutionLike record);
    int updateActiveState(@Param("solutionId") Long solutionId, @Param("userId") Long userId,
                          @Param("active") boolean active, @Param("updatedAt") LocalDateTime updatedAt);
    int markFirstNotified(@Param("solutionId") Long solutionId, @Param("userId") Long userId,
                          @Param("updatedAt") LocalDateTime updatedAt);
    long countActiveBySolution(@Param("solutionId") Long solutionId);
    List<Long> selectActiveSolutionIdsForUser(@Param("userId") Long userId,
                                               @Param("solutionIds") List<Long> solutionIds);
    List<Long> selectActiveUserIds(@Param("solutionId") Long solutionId, @Param("userIds") List<Long> userIds);
}
