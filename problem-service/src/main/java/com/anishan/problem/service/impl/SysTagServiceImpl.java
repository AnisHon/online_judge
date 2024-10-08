package com.anishan.problem.service.impl;

import com.anishan.commons.entity.dto.PagedQuery;
import com.anishan.commons.entity.vo.PagedResult;
import com.anishan.problem.entity.dto.SysTagDto;
import com.anishan.problem.entity.po.SysTag;
import com.anishan.problem.entity.vo.SysTagVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.problem.service.SysTagService;
import com.anishan.problem.mapper.SysTagMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author anishan
* @description 针对表【sys_tag(题目标签表)】的数据库操作Service实现
* @createDate 2024-10-08 14:59:21
*/
@Service
public class SysTagServiceImpl extends ServiceImpl<SysTagMapper, SysTag>
    implements SysTagService{

    @Override
    public SysTagVo getTagById(Long id) {
        return null;
    }

    @Override
    public List<SysTagVo> listTagById(List<Long> ids) {
        return List.of();
    }

    @Override
    public PagedResult<SysTagVo> listTages(PagedQuery<SysTag> pagedQuery) {
        return null;
    }

    @Override
    public boolean updateTag(SysTagDto tagDto) {
        return false;
    }

    @Override
    public void addTag(SysTagDto tagDto) {

    }
}




