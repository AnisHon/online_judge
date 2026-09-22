package com.anishan.problem.mapper;

import com.anishan.problem.domain.vo.ProfileContestVo;
import com.anishan.problem.domain.vo.ProfileProblemItemVo;
import com.anishan.problem.domain.vo.ProfileSolutionVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 个人主页的聚合查询，所有明细查询都带有服务端上限。 */
public interface ProfileMapper {
    long countSolved(@Param("userId") Long userId);

    long countAttempted(@Param("userId") Long userId);

    List<ProfileProblemItemVo> selectSolvedProblems(@Param("userId") Long userId, @Param("limit") int limit);

    List<ProfileContestVo> selectContests(@Param("userId") Long userId, @Param("limit") int limit);

    List<ProfileSolutionVo> selectSolutions(@Param("userId") Long userId,
                                            @Param("includePrivate") boolean includePrivate,
                                            @Param("limit") int limit);
}
