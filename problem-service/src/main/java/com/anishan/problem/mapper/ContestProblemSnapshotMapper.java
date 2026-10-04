package com.anishan.problem.mapper;

import com.anishan.problem.domain.entity.ContestProblemSnapshot;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.math.BigDecimal;

public interface ContestProblemSnapshotMapper extends BaseMapper<ContestProblemSnapshot> {
    List<ContestProblemSnapshot> selectByContestId(@Param("contestId") Long contestId);

    int deleteByContestId(@Param("contestId") Long contestId);

    BigDecimal selectMaxScore(@Param("contestId") Long contestId,
                              @Param("problemId") Long problemId);
}
