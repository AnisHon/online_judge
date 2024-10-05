package com.anishan.user.service;

import com.anishan.user.entity.po.StudentClassRelation;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author anishan
* @description 针对表【student_class(学生班级关系表)】的数据库操作Service
* @createDate 2024-10-06 00:41:19
*/
public interface StudentClassService extends IService<StudentClassRelation> {

    boolean joinClass(Long userId, Long classId);

    boolean quitClass(Long userId, Long classId);

    List<Long> listClassIdsOfUser(Long userId);

    List<Long> listStudentIdsByClass(Long classId);
}
