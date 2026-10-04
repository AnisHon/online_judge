package com.anishan.problem.mapper;

import com.anishan.problem.domain.entity.SupplementContest;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
* @author happy
* @description 针对表【supplement_contest】的数据库操作Mapper
* @createDate 2025-01-18 22:45:36
* @Entity com.anishan.problem.domain.entity.SupplementContest
*/
public interface SupplementContestMapper extends BaseMapper<SupplementContest> {

    int upsertDeadline(@Param("contestId") Long contestId,
                       @Param("userId") Long userId,
                       @Param("deadline") LocalDateTime deadline);

    int deleteByContestAndUser(@Param("contestId") Long contestId, @Param("userId") Long userId);

}



