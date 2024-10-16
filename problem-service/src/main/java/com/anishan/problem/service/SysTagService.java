package com.anishan.problem.service;

import com.anishan.commons.entity.dto.PagedQuery;
import com.anishan.commons.entity.vo.PagedResult;
import com.anishan.problem.entity.dto.SysTagDto;
import com.anishan.problem.entity.po.SysTag;
import com.anishan.problem.entity.vo.SysTagVo;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
* @author anishan
* @description 针对表【sys_tag(题目标签表)】的数据库操作Service
* @createDate 2024-10-08 14:59:21
*/
public interface SysTagService extends IService<SysTag> {

    SysTagVo getTagById(@NotNull(message = "id为Null") Long id);

    List<SysTagVo> listTagById(List<Long> ids);

    PagedResult<SysTagVo> listTages(PagedQuery<SysTag> pagedQuery);

    boolean updateTag(SysTagDto tagDto);

    LocalDateTime getUpdateTime(Long id);

    boolean addTag(SysTagDto tagDto);
}
