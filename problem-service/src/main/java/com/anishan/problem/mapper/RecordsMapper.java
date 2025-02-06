package com.anishan.problem.mapper;

import com.anishan.problem.domain.entity.Records;
import com.anishan.problem.domain.vo.ProblemScore;
import com.anishan.problem.domain.vo.ProblemStatistic;
import com.anishan.problem.domain.vo.UserScore;
import com.anishan.problem.domain.vo.UserStatistic;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.security.core.parameters.P;

import java.util.List;

/**
* @author happy
* @description 针对表【records(题目完成表)】的数据库操作Mapper
* @createDate 2024-10-16 22:39:16
* @Entity com.anishan.problem.entity.Records
*/
public interface RecordsMapper extends MPJBaseMapper<Records> {

    /**
     * 题目正误情况
     */
    List<ProblemStatistic> selectProblemStatistic(@Param("contestId") Long contestId);

    /**
     * 用户分数情况
     * todo Contest没有完善
     */
    List<UserStatistic> selectUserStatistic(@Param("contestId") Long contestId);

    /**
     * 单独一道题目的用户分数情况
     */
    List<ProblemScore> selectProblemScore(@Param("problemId") Long problemId, @Param("contestId") Long contestId);

    /**
     * 用户每道题的正误情况
     */
    List<UserScore> selectUserScore(@Param("userId") Long userId, @Param("contestId") Long contestId, @Param("listId") Long listId);
}




