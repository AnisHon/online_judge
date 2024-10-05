package com.anishan.user.service;

import com.anishan.user.entity.po.TeacherClassRelation;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author anishan
* @description 针对表【teacher_class(教师班级关系表)】的数据库操作Service
* @createDate 2024-10-06 01:47:22
*/
public interface TeacherClassService extends IService<TeacherClassRelation> {

    List<Long> listClassIdOfTeacher(Long userId);

    void addTeacherForClass(Long userId, Long classId);
}
