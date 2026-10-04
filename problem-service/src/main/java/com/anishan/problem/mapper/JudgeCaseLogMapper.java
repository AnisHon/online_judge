package com.anishan.problem.mapper;

import com.anishan.problem.domain.entity.JudgeCaseLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface JudgeCaseLogMapper extends BaseMapper<JudgeCaseLog> {
    JudgeCaseLog selectBySubmitIdAndCaseIndex(@Param("submitId") Long submitId,
                                              @Param("caseIndex") Integer caseIndex);

    List<JudgeCaseLog> selectBySubmitId(@Param("submitId") Long submitId);

    List<JudgeCaseLog> selectBySubmitIdForUpdate(@Param("submitId") Long submitId);
}
