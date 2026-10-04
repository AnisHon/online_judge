package com.anishan.problem.mapper;


import com.anishan.problem.domain.entity.SubmitLog;
import com.anishan.commons.enumeration.JudgeResult;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
* @author happy
* @description 针对表【submit_log(OJ判题提交记录)】的数据库操作Mapper
* @createDate 2024-10-16 22:39:16
* @Entity com.anishan.problem.entity.SubmitLog
*/
public interface SubmitLogMapper extends BaseMapper<SubmitLog> {

    SubmitLog selectByIdForUpdate(@Param("submitId") Long submitId);

    Long selectContestIdBySubmitId(@Param("submitId") Long submitId);

    int updateIntermediateStatus(@Param("submitId") Long submitId,
                                 @Param("status") JudgeResult status);

    int applyFinalResult(@Param("submitId") Long submitId,
                         @Param("status") JudgeResult status,
                         @Param("time") Long time,
                         @Param("memory") Long memory,
                         @Param("stderr") String stderr,
                         @Param("internalError") String internalError,
                         @Param("errorCode") String errorCode,
                         @Param("totalCount") Integer totalCount,
                         @Param("passCount") Integer passCount,
                         @Param("score") BigDecimal score,
                         @Param("completedAt") LocalDateTime completedAt);

}


