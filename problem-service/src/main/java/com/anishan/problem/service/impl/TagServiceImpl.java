package com.anishan.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.anishan.problem.domain.dto.ProblemTagDto;
import com.anishan.problem.domain.dto.TagDto;
import com.anishan.problem.domain.entity.ProblemTagRelation;
import com.anishan.problem.domain.vo.TagVo;
import com.anishan.problem.service.ProblemTagService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.domain.entity.Tag;
import com.anishan.problem.service.TagService;
import com.anishan.problem.mapper.TagMapper;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
* @author happy
* @description 针对表【tag(题目标签表)】的数据库操作Service实现
* @createDate 2024-10-16 22:39:16
*/
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService{

    private final ProblemTagService problemTagService;

    public LocalDateTime getUpdateTime(Long tagId) {
        return this.getObj(new LambdaQueryWrapper<Tag>()
                        .select(Tag::getUpdateTime)
                        .eq(Tag::getTagId, tagId),
                o -> (LocalDateTime) o
        );
    }

    public boolean doUpdate(Tag tag) {
        LocalDateTime updateTime = getUpdateTime(tag.getTagId());
        tag.setUpdateTime(updateTime);
        return this.updateById(tag);
    }

    public void doCheckName(TagDto tagDto) {
        boolean exists = this.exists(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getTagName, tagDto.getTagName())
        );
        if (exists) {
            throw new RuntimeException("标签名字重复");
        }
    }

    @Override
    public List<TagVo> getAll() {
        List<Tag> tags = this.list();
        return BeanUtil.copyToList(tags, TagVo.class);
    }

    @Override
    public boolean addTag(TagDto tag) {
        doCheckName(tag);

        Tag tag1 = BeanUtil.copyProperties(tag, Tag.class, "tagId");
        return this.save(tag1);
    }

    @Override
    public boolean updateTag(TagDto tagDto) {
        Tag tag = this.getById(tagDto.getTagId());
        BeanUtil.copyProperties(tagDto, tag);
        return doUpdate(tag);
    }

    @Override
    public boolean deleteTag(Long id) {

        return this.removeById(id);
    }

    @Override
    public boolean addTagForProblem(ProblemTagDto problemTagDto) {

        ProblemTagRelation problemTagRelation = BeanUtil.copyProperties(problemTagDto, ProblemTagRelation.class);
        return problemTagService.save(problemTagRelation);
    }

    @Override
    public boolean removeTagForProblem(ProblemTagDto problemTagDto) {
        return problemTagService.remove(new LambdaQueryWrapper<ProblemTagRelation>()
                .eq(ProblemTagRelation::getProblemId, problemTagDto.getProblemId())
                .eq(ProblemTagRelation::getTagId, problemTagDto.getTagId())
        );
    }


    @Override
    public List<TagVo> getBatchById(List<Long> ids) {
        if (CollectionUtil.isEmpty(ids)) {
            return new ArrayList<>();
        }
        List<Tag> batchById = this.listByIds(ids);
        return BeanUtil.copyToList(batchById, TagVo.class);
    }

    @Override
    public List<Long> getProblemTagIds(Long problem_id) {
        return problemTagService.listObjs(new LambdaQueryWrapper<ProblemTagRelation>()
                        .select(ProblemTagRelation::getTagId)
                        .eq(ProblemTagRelation::getProblemId, problem_id),
                x -> (Long) x
        );
    }


    @Override
    public List<TagVo> getTagByProblemId(Long problemId) {
        List<Long> problemTagIds = getProblemTagIds(problemId);
        return getBatchById(problemTagIds);
    }

    @Override
    @Transactional
    public boolean batchAddTagsForProblem(List<ProblemTagDto> relations) {
        boolean result = !CollectionUtil.isEmpty(relations);
        for (ProblemTagDto relation : relations) {
            result &= addTagForProblem(relation);
        }
        return result;
    }

    @Override
    public boolean batchRemoveTagsForProblem(List<ProblemTagDto> relations) {
        boolean result = !CollectionUtil.isEmpty(relations);
        for (ProblemTagDto relation : relations) {
            result &= removeTagForProblem(relation);
        }
        return result;
    }


}




