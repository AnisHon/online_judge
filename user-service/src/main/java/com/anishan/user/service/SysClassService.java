package com.anishan.user.service;

import com.anishan.commons.entity.dto.PagedQuery;
import com.anishan.commons.entity.vo.PagedResult;
import com.anishan.user.domain.dto.ClassDto;
import com.anishan.user.domain.dto.ClassPagedQuery;
import com.anishan.user.domain.entity.SysClass;
import com.anishan.user.domain.vo.BinaryResultOv;
import com.anishan.user.domain.vo.ClassVo;
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

    // join class
    BinaryResultOv joinClass(Long userId, Long clasId);

    BinaryResultOv joinClass(Long clasId);

    BinaryResultOv quitClass(Long userId, Long classId);

    BinaryResultOv quitClass(Long classId);

    PagedResult<ClassVo> listClassOfUser(ClassPagedQuery classPagedQuery);

    PagedResult<ClassVo> listClassOfUser(ClassPagedQuery classPagedQuery, Long userId);

    PagedResult<ClassVo> listClassOfTeacher(ClassPagedQuery classPagedQuery);

    PagedResult<ClassVo> listClassOfTeacher(ClassPagedQuery classPagedQuery, Long userId);

    boolean createClass(ClassDto classDto);
}
