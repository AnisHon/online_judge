package com.anishan.problem.mapper;

import com.anishan.problem.domain.entity.SolutionExplanation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.interfaces.MPJBaseJoin;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 文件信息表 Mapper 接口
 * </p>
 *
 * @author anishan
 * @since 2025-01-11
 */
public interface SolutionExplanationMapper extends MPJBaseMapper<SolutionExplanation> {

    boolean deleteBatch(@Param("ids") List<Long> ids, @Param("userId") Long userId);
}
