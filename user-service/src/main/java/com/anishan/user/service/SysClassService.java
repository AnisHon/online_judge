package com.anishan.user.service;

import com.anishan.commons.entity.dto.PagedQuery;
import com.anishan.commons.entity.vo.PagedResult;
import com.anishan.user.entity.dto.ClassDto;
import com.anishan.user.entity.dto.ClassPagedQuery;
import com.anishan.user.entity.po.SysClass;
import com.anishan.user.entity.vo.ClassVo;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
* @author anishan
* @description 针对表【sys_class(班级信息表)】的数据库操作Service
* @createDate 2024-10-03 01:02:36
*/
public interface SysClassService extends IService<SysClass> {

    ClassVo getClassById(@NotNull(message = "id为Null") Long id);

    List<ClassVo> listClassById(List<Long> ids);

    PagedResult<ClassVo> listClasses(PagedQuery<SysClass> pagedQuery);

    PagedResult<ClassVo> queryClass(ClassPagedQuery classPagedQuery);

    boolean updateClass(ClassDto classDto);

    boolean addClass(ClassDto sysClassDto);
}
