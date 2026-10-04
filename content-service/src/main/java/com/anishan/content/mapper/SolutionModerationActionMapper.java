package com.anishan.content.mapper;

import com.anishan.content.domain.entity.SolutionModerationAction;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.anishan.content.domain.vo.ModerationActionVo;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface SolutionModerationActionMapper extends BaseMapper<SolutionModerationAction> {
    List<ModerationActionVo> selectOwnSolutionActions(@Param("solutionId") Long solutionId,
                                                     @Param("authorId") Long authorId);
    List<ModerationActionVo> selectOwnCommentActions(@Param("commentId") Long commentId,
                                                    @Param("authorId") Long authorId);
    List<SolutionModerationAction> selectCommentActionsByTargets(@Param("targetIds") List<Long> targetIds);
    long countHistory(@Param("solutionId") Long solutionId);
    List<ModerationActionVo> selectHistory(@Param("solutionId") Long solutionId,
                                         @Param("offset") long offset, @Param("limit") long limit);
}
