package com.anishan.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.user.domain.entity.TeacherClassRelation;
import com.anishan.user.service.TeacherClassService;
import com.anishan.user.mapper.TeacherClassMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author anishan
* @description 针对表【teacher_class(教师班级关系表)】的数据库操作Service实现
* @createDate 2024-10-06 01:47:22
*/
@Service
public class TeacherClassServiceImpl extends ServiceImpl<TeacherClassMapper, TeacherClassRelation>
    implements TeacherClassService{


    @Override
    public List<Long> listClassIdOfTeacher(Long userId) {
        return this.listObjs(new LambdaQueryWrapper<TeacherClassRelation>()
                .select(TeacherClassRelation::getClassId)
                .eq(TeacherClassRelation::getTeacherId, userId));

    }

    @Override
    public void addTeacherForClass(Long userId, Long classId) {
        TeacherClassRelation teacherClassRelation = new TeacherClassRelation(userId, classId);
        this.save(teacherClassRelation);
    }
}




