package com.anishan.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.anishan.problem.domain.dto.ProblemTagDto;
import com.anishan.problem.domain.dto.TagDto;
import com.anishan.problem.domain.entity.ProblemTagRelation;
import com.anishan.problem.domain.vo.TagVo;
import com.anishan.problem.service.ProblemTagService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.domain.entity.Tag;
import com.anishan.problem.service.TagService;
import com.anishan.problem.mapper.TagMapper;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    private boolean doCheckName(String name) {
        return this.exists(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getTagName, name)
        );
    }

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

    public boolean doCheckIdExists(Long id) {
       return this.exists(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getTagName, id)
        );
    }

    public void doCheckName(TagDto tagDto) {
        boolean exists = this.exists(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getTagName, tagDto.getTagName())
        );
        if (exists) {
            throw new RuntimeException("标签名字重复");
        }
    }

    public void doCheckBeforeUpdate(TagDto tagDto) {
        doCheckIdAndThrow(tagDto.getTagId());
        boolean exists = doCheckName(tagDto.getTagName());
        if (!exists) {
            throw new RuntimeException("标签名字已经存在");
        }


        doCheckName(tagDto);
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
        doCheckBeforeUpdate(tagDto);
        Tag tag = BeanUtil.copyProperties(tagDto, Tag.class);
        return doUpdate(tag);
    }

    @Override
    public boolean deleteTag(Long id) {
        boolean b = doCheckIdExists(id);
        if (!b) {
            throw new RuntimeException("删除标签不存在");
        }
        return this.removeById(id);
    }

    public void doCheckIdAndThrow(Long id) {
        boolean b = doCheckIdExists(id);
        if (!b) {
            throw new RuntimeException("ID不存在");
        }
    }

    @Override
    public boolean addTagForProblem(ProblemTagDto problemTagDto) {
        doCheckIdExists(problemTagDto.getTagId());
        doCheckIdExists(problemTagDto.getTagId());
        ProblemTagRelation problemTagRelation = BeanUtil.copyProperties(problemTagDto, ProblemTagRelation.class);
        return problemTagService.save(problemTagRelation);
    }

    @Override
    public boolean removeTagForProblem(ProblemTagDto problemTagDto) {
        ProblemTagRelation problemTagRelation = BeanUtil.copyProperties(problemTagDto, ProblemTagRelation.class);
        return problemTagService.removeById(problemTagRelation);
    }


}




