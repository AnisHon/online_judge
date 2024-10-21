package com.anishan.problem.mapper;

import com.anishan.problem.domain.entity.Problem;
import com.anishan.problem.domain.vo.TaggedProblemVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author happy
* @description 针对表【problem(题目主表，OJ题目有分表，非OJ不需要继续分表)】的数据库操作Mapper
* @createDate 2024-10-16 22:39:16
* @Entity com.anishan.problem.entity.Problem
*/
public interface ProblemMapper extends BaseMapper<Problem> {
//    List<Problem> s()
    List<Problem> selectAllByProblemIdAndTagId(Page<Problem> page, @Param("problemId") Long problemId, @Param("tagIds") List<Long> tagIds);
    Long selectAllCountByProblemIdAndTagId(@Param("problemId") Long problemId, @Param("tagIds") List<Long> tagIds);
    List<TaggedProblemVo> selectTaggedProblemByProblemIdAndTagId(Page<Problem> page, @Param("problemId") Long problemId, @Param("tagIds") List<Long> tagIds);
    Long selectTaggedProblemCountByProblemIdAndTagId(@Param("problemId") Long problemId, @Param("tagIds") List<Long> tagIds);
}




