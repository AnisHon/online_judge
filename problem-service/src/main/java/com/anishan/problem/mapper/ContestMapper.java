package com.anishan.problem.mapper;

import com.anishan.problem.domain.entity.Contest;
import com.anishan.problem.domain.entity.UserContestRelation;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author happy
* @description 针对表【contest(比赛表)】的数据库操作Mapper
* @createDate 2024-10-16 22:39:15
* @Entity com.anishan.problem.entity.Contest
*/
public interface ContestMapper extends MPJBaseMapper<Contest> {

    int deleteBatchUserContestRelations(@Param("relations") List<UserContestRelation> relations);
}




