package com.anishan.problem.mapper;

import com.anishan.problem.domain.entity.ContestRecords;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;


/**
 * <p>
 * 比赛题目记录 Mapper 接口
 * </p>
 *
 * @author anishan
 * @since 2025-01-07
 */
public interface ContestRecordsMapper extends BaseMapper<ContestRecords> {

    ContestRecords selectForUpdate(@Param("contestId") Long contestId,
                                   @Param("userId") Long userId,
                                   @Param("problemId") Long problemId);

    int insertDraftIfAbsent(@Param("recordId") Long recordId,
                            @Param("contestId") Long contestId,
                            @Param("userId") Long userId,
                            @Param("problemId") Long problemId);

    int upsertGradedProjection(@Param("recordId") Long recordId,
                               @Param("contestId") Long contestId,
                               @Param("userId") Long userId,
                               @Param("problemId") Long problemId,
                               @Param("status") Boolean status,
                               @Param("score") BigDecimal score,
                               @Param("attemptSeq") Long attemptSeq,
                               @Param("attemptId") Long attemptId);

    int upsertLegacyGrade(@Param("recordId") Long recordId,
                          @Param("contestId") Long contestId,
                          @Param("userId") Long userId,
                          @Param("problemId") Long problemId,
                          @Param("status") Boolean status,
                          @Param("score") BigDecimal score);

    BigDecimal selectContestScore(@Param("contestId") Long contestId,
                                  @Param("userId") Long userId);

}
