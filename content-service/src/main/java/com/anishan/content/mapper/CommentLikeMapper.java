package com.anishan.content.mapper;

import com.anishan.content.domain.entity.CommentLike;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CommentLikeMapper {
    CommentLike selectByCommentAndUser(@Param("commentId") Long commentId, @Param("userId") Long userId);
    CommentLike selectByCommentAndUserForUpdate(@Param("commentId") Long commentId,
                                                @Param("userId") Long userId);
    int insertState(@Param("record") CommentLike record);
    int updateActiveState(@Param("commentId") Long commentId, @Param("userId") Long userId,
                          @Param("active") boolean active, @Param("updatedAt") LocalDateTime updatedAt);
    int markFirstNotified(@Param("commentId") Long commentId, @Param("userId") Long userId,
                          @Param("updatedAt") LocalDateTime updatedAt);
    long countActiveByComment(@Param("commentId") Long commentId);
    List<Long> selectActiveUserIds(@Param("commentId") Long commentId, @Param("userIds") List<Long> userIds);
    List<Long> selectActiveCommentIdsForUser(@Param("userId") Long userId,
                                              @Param("commentIds") List<Long> commentIds);
}
