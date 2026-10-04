package com.anishan.problem.mapper;

import com.anishan.problem.domain.entity.ContestRankEntry;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** Rank entries are generated as a single database-side aggregate per immutable version. */
public interface ContestRankEntryMapper extends BaseMapper<ContestRankEntry> {

    int insertCandidate(@Param("contestId") Long contestId,
                        @Param("version") Long version,
                        @Param("createdAt") LocalDateTime createdAt);

    long countByContestVersion(@Param("contestId") Long contestId, @Param("version") Long version);

    long countRosterUsers(@Param("contestId") Long contestId);

    List<ContestRankEntry> selectPageByVersion(@Param("contestId") Long contestId,
                                               @Param("version") Long version,
                                               @Param("offset") long offset,
                                               @Param("pageSize") int pageSize);

    int deleteUnretainedVersions(@Param("contestId") Long contestId,
                                 @Param("publishedVersion") Long publishedVersion,
                                 @Param("protectedCandidate") Long protectedCandidate,
                                 @Param("batchSize") int batchSize);
}
