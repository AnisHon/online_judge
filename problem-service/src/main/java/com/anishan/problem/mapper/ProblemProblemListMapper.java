package com.anishan.problem.mapper;

import com.anishan.problem.domain.dto.ProblemListRelationDto;
import com.anishan.problem.domain.dto.ProblemListOrderItemDto;
import com.anishan.problem.domain.entity.ProblemListRelation;
import com.anishan.problem.domain.entity.ProblemProblemListRelation;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

/**
* @author happy
* @description 针对表【problem_problem_list(题单 题目关系表)】的数据库操作Mapper
* @createDate 2024-10-16 22:40:59
* @Entity com.anishan.problem.entity.ProblemProblemListRelation
*/
public interface ProblemProblemListMapper extends MPJBaseMapper<ProblemProblemListRelation> {
    List<ProblemListRelation> getByListId(Long listId);

    int deleteBatch(@Param("relations") List<ProblemListRelationDto> relations);

    int updateProblemOrderBatch(@Param("listId") Long listId,
                                @Param("items") List<ProblemListOrderItemDto> items);

    @Select({"<script>",
            "select count(distinct c.contest_id)",
            "from contest c join problem_problem_list ppl on ppl.list_id = c.list_id",
            "where c.del_flag = 0",
            "and ppl.problem_id in",
            "<foreach collection='problemIds' item='problemId' open='(' separator=',' close=')'>#{problemId}</foreach>",
            "</script>"})
    long countEventsUsingProblems(@Param("problemIds") List<Long> problemIds);

    @Delete({"<script>",
            "delete from problem_problem_list where problem_id in",
            "<foreach collection='problemIds' item='problemId' open='(' separator=',' close=')'>#{problemId}</foreach>",
            "</script>"})
    int deleteByProblemIds(@Param("problemIds") List<Long> problemIds);
}

