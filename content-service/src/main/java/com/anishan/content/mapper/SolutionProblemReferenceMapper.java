package com.anishan.content.mapper;

import com.anishan.content.domain.entity.SolutionProblemReference;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SolutionProblemReferenceMapper extends BaseMapper<SolutionProblemReference> {
    int upsertFenced(@Param("reference") SolutionProblemReference reference);

    List<SolutionProblemReference> selectByProblemIds(@Param("problemIds") List<Long> problemIds);

    List<SolutionProblemReference> selectRefreshCandidates(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("cursorCheckedAt") LocalDateTime cursorCheckedAt,
            @Param("cursorProblemId") Long cursorProblemId,
            @Param("limit") int limit);
}
