package com.anishan.content.mapper;

import com.anishan.content.domain.entity.SolutionExplanationContent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

public interface SolutionExplanationContentMapper extends BaseMapper<SolutionExplanationContent> {
    String selectContentBySolutionId(@Param("solutionId") Long solutionId);

    int upsertContent(@Param("solutionId") Long solutionId, @Param("content") String content);
}
