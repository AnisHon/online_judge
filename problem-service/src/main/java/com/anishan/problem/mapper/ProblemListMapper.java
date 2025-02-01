package com.anishan.problem.mapper;

import com.anishan.problem.domain.entity.ProblemList;
import com.anishan.problem.domain.vo.ProblemInListVo;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author happy
* @description 针对表【problem_list(题单表)】的数据库操作Mapper
* @createDate 2024-10-16 22:39:16
* @Entity com.anishan.problem.entity.ProblemList
*/
public interface ProblemListMapper extends MPJBaseMapper<ProblemList> {


    List<ProblemInListVo> selectProblemByListId(@Param("contestId") Long contestId, @Param("userId") Long userId, @Param("listId") Long listId);
}




