package com.anishan.user.controller;

import com.anishan.commons.domain.R;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.user.domain.dto.ClassDto;
import com.anishan.user.domain.dto.ClassPagedQuery;
import com.anishan.user.domain.dto.UserPagedQuery;
import com.anishan.user.domain.vo.BinaryResultOv;
import com.anishan.user.domain.vo.ClassVo;
import com.anishan.api.client.user.domain.vo.UserVo;
import com.anishan.user.service.*;
import io.swagger.annotations.ApiOperation;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/teacher")
public class TeacherController {

    private final SysUserService sysUserService;
    private final SysClassService sysClassService;
    public TeacherController(
            SysUserService sysUserService,
            SysClassService sysClassService
    ) {
        this.sysUserService = sysUserService;
        this.sysClassService = sysClassService;
    }

    // create class
    @PostMapping("/create-class")
    @ApiOperation("创建班级，如果失败msg就是原因")
    @PreAuthorize("hasAuthority('user:teacher:create-class')")
    public R<BinaryResultOv> createClass(@RequestBody @Validated ClassDto classDto) {
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
    public R<BinaryResultOv> removeClass(@PathVariable("classId") @Validated Long classId) {
        boolean b = sysClassService.removeById(classId);
        return BinaryResultOv.ternary(b, "删除成功", "删除失败，班级不存在").tOvR();
    }

    // todo
    // send homework (class_id, problem_set)

    // homeworks

    // homework statistic (problem_set, homework_id)

    // problem -> student (problem, homework_id)


}
