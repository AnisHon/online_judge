package com.anishan.user.controller;

import com.anishan.commons.domain.R;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.user.domain.dto.ClassPagedQuery;
import com.anishan.user.domain.vo.BinaryResultOv;
import com.anishan.user.domain.vo.ClassVo;
import com.anishan.user.service.SysClassService;
import com.anishan.user.service.SysUserService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class StudentController {

    private final SysClassService classService;



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
    @PostMapping("/list-classes")
    @ApiOperation("列出用户加入的班级")
    public R<PagedResult<ClassVo>> listClasses(@RequestBody @Validated ClassPagedQuery classPagedQuery) {
        return classService.listClassOfUser(classPagedQuery).toR();
    }


    // my homework
    // todo



}
