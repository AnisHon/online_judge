package com.anishan.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.user.domain.entity.StudentClassRelation;
import com.anishan.user.service.StudentClassService;
import com.anishan.user.mapper.StudentClassMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author anishan
* @description 针对表【student_class(学生班级关系表)】的数据库操作Service实现
* @createDate 2024-10-06 00:41:19
*/
@Service
public class StudentClassServiceImpl extends ServiceImpl<StudentClassMapper, StudentClassRelation>
    implements StudentClassService{

    @Override
    public boolean joinClass(Long userId, Long classId) {
        StudentClassRelation studentClassRelation = new StudentClassRelation(userId, classId);

        boolean save = false;
        try {
            save = this.save(studentClassRelation);
        } catch (Exception ignore) {}

        return save;
    }

    @Override
    public boolean quitClass(Long userId, Long classId) {
        return this.remove(new LambdaQueryWrapper<StudentClassRelation>()
                .eq(StudentClassRelation::getStudentId, userId)
                .eq(StudentClassRelation::getClassId, classId)
        );
    }

    @Override
    public List<Long> listClassIdsOfUser(Long userId) {
        return this.listObjs(new LambdaQueryWrapper<StudentClassRelation>()
                .select(StudentClassRelation::getClassId)
                .eq(StudentClassRelation::getStudentId, userId)
        );

    }

    @Override
    public List<Long> listStudentIdsByClass(Long classId) {
       return this.listObjs(new LambdaQueryWrapper<StudentClassRelation>()
                .select(StudentClassRelation::getStudentId)
                .eq(StudentClassRelation::getClassId, classId)
        );
    }

}




