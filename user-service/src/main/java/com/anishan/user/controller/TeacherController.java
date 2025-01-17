package com.anishan.user.controller;

import com.anishan.commons.domain.R;
import com.anishan.user.domain.dto.ClassDto;
import com.anishan.user.service.SysClassService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final SysClassService sysClassService;

    // create class
    @PostMapping("/createClass")
    @ApiOperation("创建班级，如果失败msg就是原因")
    @PreAuthorize("hasAuthority('user:teacher:create-class')")
    public R<Boolean> createClass(@RequestBody @Validated ClassDto classDto) {
        boolean b = sysClassService.createClass(classDto);
        return R.success(b);
    }

    // delete class
    @DeleteMapping("/{classId}")
    @ApiOperation("删除班级，如果失败msg就是原因")
    @PreAuthorize("hasAuthority('user:teacher:remove-class')")
    public R<Boolean> removeClass(@PathVariable("classId") @Validated Long classId) {
        boolean b = sysClassService.removeById(classId);
        return R.success(b);
    }

    // todo
    // send homework (class_id, problem_set)

    // homeworks

    // homework statistic (problem_set, homework_id)

    // problem -> student (problem, homework_id)


}
