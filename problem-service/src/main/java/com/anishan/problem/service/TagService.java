package com.anishan.problem.service;

import com.anishan.problem.domain.dto.ProblemTagDto;
import com.anishan.problem.domain.dto.TagDto;
import com.anishan.problem.domain.entity.Tag;
import com.anishan.problem.domain.vo.TagVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author happy
* @description 针对表【tag(题目标签表)】的数据库操作Service
* @createDate 2024-10-16 22:39:16
*/
public interface TagService extends IService<Tag> {

    List<TagVo> getAll();

    boolean addTag(TagDto tag);

    boolean updateTag(TagDto tagDto);

    boolean deleteTag(Long id);

    boolean addTagForProblem(ProblemTagDto problemTagDto);

    boolean removeTagForProblem(ProblemTagDto problemTagDto);

    List<TagVo> getBatchById(List<Long> ids);

    List<Long> getProblemTagIds(Long problem_id);

    List<TagVo> getTagByProblemId(Long problemId);
}
