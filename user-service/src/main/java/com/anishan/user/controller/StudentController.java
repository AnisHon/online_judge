package com.anishan.user.controller;

import com.anishan.commons.entity.R;
import com.anishan.commons.entity.vo.PagedResult;
import com.anishan.user.entity.dto.ClassPagedQuery;
import com.anishan.user.entity.vo.BinaryResultOv;
import com.anishan.user.entity.vo.ClassVo;
import com.anishan.user.service.SysClassService;
import com.anishan.user.service.SysUserService;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/student")
public class StudentController {

    SysUserService sysUserService;
    SysClassService classService;
    public StudentController(
            SysUserService sysUserService,
            SysClassService classService
    ) {
        this.sysUserService = sysUserService;
        this.classService = classService;
    }



    // join class
    @GetMapping("/join-class/{classId}")
    @ApiOperation("加入班级，如果失败msg就是原因")
    public R<BinaryResultOv> joinClass(@PathVariable("classId") Long classId) {
        return classService.joinClass(classId).tOvR();
    }

    // quit class
    @GetMapping("/quit-class/{classId}")
    @ApiOperation("退出班级，如果失败msg就是原因")
    public R<BinaryResultOv> quitClass(@PathVariable("classId") Long classId) {
        return classService.quitClass(classId).tOvR();
    }

    // my class
    @GetMapping("/list-classes")
    @ApiOperation("列出用户加入的班级")
    public R<PagedResult<ClassVo>> listClasses(@Validated ClassPagedQuery classPagedQuery) {
        return classService.listClassOfUser(classPagedQuery).toR();
    }

    // my homework
    // todo



}
