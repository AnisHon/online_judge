package com.anishan.problem.mapper;

import com.anishan.problem.domain.entity.Records;
import com.anishan.problem.domain.vo.ProblemStatistic;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author happy
* @description 针对表【records(题目完成表)】的数据库操作Mapper
* @createDate 2024-10-16 22:39:16
* @Entity com.anishan.problem.entity.Records
*/
public interface RecordsMapper extends MPJBaseMapper<Records> {

    List<ProblemStatistic> statistic(@Param("contestId") Long contestId);
}




