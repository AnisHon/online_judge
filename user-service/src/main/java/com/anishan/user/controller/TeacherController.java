package com.anishan.user.controller;

import com.anishan.api.entity.SysUser;
import com.anishan.commons.entity.R;
import com.anishan.commons.entity.dto.PagedQuery;
import com.anishan.commons.entity.vo.PagedResult;
import com.anishan.user.entity.dto.ClassDto;
import com.anishan.user.entity.dto.ClassPagedQuery;
import com.anishan.user.entity.dto.UserPagedQuery;
import com.anishan.user.entity.vo.BinaryResultOv;
import com.anishan.user.entity.vo.ClassVo;
import com.anishan.user.entity.vo.UserVo;
import com.anishan.user.service.*;
import com.anishan.user.service.impl.AuthenticationServiceImpl;
import com.anishan.user.util.UserUtil;
import io.swagger.annotations.ApiOperation;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/teacher")
public class TeacherController {

    private final AuthenticationServiceImpl authenticationServiceImpl;
    SysUserService sysUserService;
    SysClassService sysClassService;
    StudentClassService studentClassService;
    SysUserRoleService sysUserRoleService;
    TeacherClassService teacherClassService;
    public TeacherController(
            SysUserService sysUserService,
            SysClassService sysClassService,
            StudentClassService studentClassService,
            SysUserRoleService sysUserRoleService,
            TeacherClassService teacherClassService,
            AuthenticationServiceImpl authenticationServiceImpl) {
        this.sysUserService = sysUserService;
        this.sysClassService = sysClassService;
        this.studentClassService = studentClassService;
        this.sysUserRoleService = sysUserRoleService;
        this.teacherClassService = teacherClassService;
        this.authenticationServiceImpl = authenticationServiceImpl;
    }


    // my class
    @GetMapping("/list-classes")
    @ApiOperation("列出用户加入的班级")
    @PreAuthorize("hasAuthority('user:teacher:list-class')")
    public R<PagedResult<ClassVo>> listClasses(@Validated ClassPagedQuery classPagedQuery) {
        return sysClassService.listClassOfTeacher(classPagedQuery).toR();
    }

    // my student (class_id)

    @PostMapping("/list")
    @ApiOperation("列出班级内所有的学生，如果classId为null就列出所有学生")
    public R<PagedResult<UserVo>> listStudents(@RequestBody UserPagedQuery userPagedQuery) {
        PagedResult<UserVo> userVoPagedResult;

        userVoPagedResult = sysUserService.listStudentsOfTeacher(userPagedQuery);

        return userVoPagedResult.toR();

    }

    // create class
    @PostMapping("/create-class")
    @ApiOperation("创建班级，如果失败msg就是原因")
    @PreAuthorize("hasAuthority('user:teacher:create-class')")
    public R<BinaryResultOv> createClass(@RequestBody ClassDto classDto) {
        try {
            boolean b = sysClassService.createClass(classDto);
            return BinaryResultOv.ternary(b, "创建成功", "失败原因未知").tOvR();
        } catch (DuplicateKeyException ignore) {
            return BinaryResultOv.fail("失败，班级名重复").tOvR();
        }

    }

    // delete class
    @GetMapping("/rm-class/{classId}")
    @ApiOperation("删除班级，如果失败msg就是原因")
    @PreAuthorize("hasAuthority('user:teacher:remove-class')")
    public R<BinaryResultOv> removeClass(@PathVariable("classId") Long classId) {
        boolean b = sysClassService.removeById(classId);
        return BinaryResultOv.ternary(b, "删除成功", "删除失败，班级不存在").tOvR();
    }

    // todo
    // send homework (class_id, problem_set)

    // homeworks

    // homework statistic (problem_set, homework_id)

    // problem -> student (problem, homework_id)


}
