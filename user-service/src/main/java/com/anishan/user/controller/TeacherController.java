package com.anishan.user.controller;

import com.anishan.user.service.SysUserService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teacher")
public class TeacherController {

    SysUserService sysUserService;
    public TeacherController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }


    // my class

    // my student (class_id)

    // send homework (class_id, problem_set)

    // homeworks

    // homework statistic (problem_set, homework_id)

    // problem -> student (problem, homework_id)


}
