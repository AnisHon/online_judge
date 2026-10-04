package com.anishan.problem.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** Finds active or still-pending activities whose score inputs use the given problems. */
@Mapper
public interface ContestMutationReferenceMapper {

    List<Long> selectBusyContestIds(@Param("problemIds") List<Long> problemIds,
                                    @Param("now") LocalDateTime now);

    /** Current-read checks lock only reference rows, never contest rows in reverse lock order. */
    List<Long> selectBusySnapshotContestIdsCurrent(@Param("problemIds") List<Long> problemIds,
                                                   @Param("now") LocalDateTime now);

    List<Long> selectBusyListContestIdsCurrent(@Param("problemIds") List<Long> problemIds,
                                               @Param("now") LocalDateTime now);

    List<Long> selectPendingContestIdsCurrent(@Param("problemIds") List<Long> problemIds);
}
