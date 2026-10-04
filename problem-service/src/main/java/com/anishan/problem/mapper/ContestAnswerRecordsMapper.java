package com.anishan.problem.mapper;

import com.anishan.problem.domain.entity.ContestAnswerRecords;
import com.anishan.problem.domain.vo.UserAnswer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 比赛完成记录表，分表专门用于存储答案 Mapper 接口
 * </p>
 *
 * @author anishan
 * @since 2025-01-07
 */
public interface ContestAnswerRecordsMapper extends BaseMapper<ContestAnswerRecords> {

    int upsertAnswer(@Param("recordId") Long recordId, @Param("answer") UserAnswer answer);

}
