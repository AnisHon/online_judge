package com.anishan.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.anishan.commons.entity.dto.PagedQuery;
import com.anishan.commons.entity.vo.PagedResult;
import com.anishan.problem.entity.dto.SysTagDto;
import com.anishan.problem.entity.po.SysTag;
import com.anishan.problem.entity.vo.SysTagVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.service.SysTagService;
import com.anishan.problem.mapper.SysTagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
* @author anishan
* @description 针对表【sys_tag(题目标签表)】的数据库操作Service实现
* @createDate 2024-10-08 14:59:21
*/
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SysTagServiceImpl extends ServiceImpl<SysTagMapper, SysTag>
    implements SysTagService{

    private final SysTagMapper sysTagMapper;

    @Override
    public SysTagVo getTagById(Long id) {
        SysTag sysTag = this.getById(id);
        return BeanUtil.copyProperties(sysTag, SysTagVo.class);
    }

    @Override
    public List<SysTagVo> listTagById(List<Long> ids) {
        if (CollectionUtil.isEmpty(ids)) {
            return new ArrayList<>();
        }

        List<SysTag> sysTags = this.listByIds(ids);
        return BeanUtil.copyToList(sysTags, SysTagVo.class);
    }

    @Override
    public PagedResult<SysTagVo> listTages(PagedQuery<SysTag> pagedQuery) {
        Page<SysTag> page = pagedQuery.page();
        page = this.page(page);
        return PagedResult.build(page, SysTagVo.class);
    }

    @Override
    public boolean updateTag(SysTagDto tagDto) {
        if (tagDto == null) {
            throw new RuntimeException("ID不能为null");
        }
        SysTag sysTag = BeanUtil.copyProperties(tagDto, SysTag.class);
        LocalDateTime updateTime = getUpdateTime(tagDto.getTagId());
        sysTag.setUpdateTime(updateTime);
        return this.updateById(sysTag);
    }

    @Override
    public LocalDateTime getUpdateTime(Long id) {
        return this.getObj(
                new LambdaQueryWrapper<SysTag>()
                .select(SysTag::getUpdateTime)
                .eq(SysTag::getTagId, id),
                (v) -> (LocalDateTime) v
        );
    }

    @Override
    public boolean addTag(SysTagDto tagDto) {
        SysTag sysTag = BeanUtil.copyProperties(tagDto, SysTag.class, "tagId");
        return this.save(sysTag);
    }
}




