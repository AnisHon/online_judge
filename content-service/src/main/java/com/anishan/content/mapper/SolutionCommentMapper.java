package com.anishan.content.mapper;

import com.anishan.content.domain.entity.SolutionComment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SolutionCommentMapper extends BaseMapper<SolutionComment> {
    SolutionComment selectByUserAndClientRequestId(@Param("userId") Long userId,
                                                    @Param("clientRequestId") String clientRequestId);

    long countRootComments(@Param("solutionId") Long solutionId);

    List<SolutionComment> selectRootComments(@Param("solutionId") Long solutionId,
                                             @Param("offset") long offset,
                                             @Param("limit") long limit);

    SolutionComment selectReplyTarget(@Param("commentId") Long commentId);

    SolutionComment selectRootForUpdate(@Param("rootId") Long rootId,
                                        @Param("solutionId") Long solutionId);

    SolutionComment selectReplyParentForUpdate(@Param("parentId") Long parentId,
                                               @Param("solutionId") Long solutionId);

    SolutionComment selectTargetForUpdate(@Param("commentId") Long commentId,
                                          @Param("solutionId") Long solutionId);

    Long selectLikeCount(@Param("commentId") Long commentId);

    int adjustLikeCount(@Param("commentId") Long commentId, @Param("delta") int delta,
                        @Param("updatedAt") java.time.LocalDateTime updatedAt);

    long countRootReplies(@Param("rootId") Long rootId);

    List<SolutionComment> selectRootReplies(@Param("rootId") Long rootId,
                                            @Param("offset") long offset,
                                            @Param("limit") long limit);

    int incrementRootReplyCount(@Param("rootId") Long rootId);

    int markVisibleDeleted(@Param("commentId") Long commentId,
                            @Param("solutionId") Long solutionId,
                            @Param("newState") String newState,
                            @Param("operatorId") Long operatorId,
                            @Param("now") java.time.LocalDateTime now);

    int decrementRootReplyCount(@Param("rootId") Long rootId);

    long countAdminComments(@Param("solutionId") Long solutionId, @Param("state") String state);

    List<SolutionComment> selectAdminComments(@Param("solutionId") Long solutionId,
                                              @Param("state") String state,
                                              @Param("offset") long offset,
                                              @Param("limit") long limit);
}
