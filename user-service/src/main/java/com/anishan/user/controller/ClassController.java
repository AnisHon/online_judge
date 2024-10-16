package com.anishan.user.controller;

import com.anishan.commons.entity.R;
import com.anishan.commons.entity.dto.PagedQuery;
import com.anishan.commons.entity.vo.PagedResult;
import com.anishan.commons.e.ValidationGroup;
import com.anishan.user.domain.dto.ClassDto;
import com.anishan.user.domain.dto.ClassPagedQuery;
import com.anishan.user.domain.entity.SysClass;
import com.anishan.user.domain.vo.ClassVo;
import com.anishan.user.service.SysClassService;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/class")
@ApiModel("班级API，增删改查")
public class ClassController {


    private final SysClassService sysClassService;
    @Autowired
    public ClassController(SysClassService sysClassService) {
        this.sysClassService = sysClassService;
    }


    @GetMapping("/get/{id}")
    @PreAuthorize("hasAuthority('user:class:list')")
    @ApiOperation("通过id获取班级")
    public R<ClassVo> getClassById(@PathVariable("id") @NotNull(message = "id为Null") Long id) {
        ClassVo clazz = sysClassService.getClassById(id);
        return R.success(clazz);
    }

    @GetMapping("/list/{ids}")
    @PreAuthorize("hasAuthority('user:class:list')")
    @ApiOperation("通过多个id获取class，id之间用','隔开 要求有user:user:get")
    public R<List<ClassVo>> listClass(@PathVariable("ids") List<Long> ids) {
        List<ClassVo> classes = sysClassService.listClassById(ids);
        return R.success(classes);
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('user:class:list')")
    @ApiOperation("分页获取class")
    public R<PagedResult<ClassVo>> listClasses(@Validated PagedQuery<SysClass> pagedQuery) {
        PagedResult<ClassVo> classVoPagedResult = sysClassService.listClasses(pagedQuery);
        return classVoPagedResult.toR();
    }

    @GetMapping("/query")
    @PreAuthorize("hasAuthority('user:class:list')")
    @ApiOperation("查询class")
    public R<PagedResult<ClassVo>> queryUser(@NotNull ClassPagedQuery classPagedQuery) {

        PagedResult<ClassVo> result = sysClassService.queryClass(classPagedQuery);

        return result.toR();
    }


    @PostMapping("/update")
    @PreAuthorize("hasAuthority('user:class:edit')")
    @ApiOperation("更新Class，不能更改classId")
    public R<Boolean> update(@RequestBody @Validated({ValidationGroup.Update.class}) ClassDto classDto) {
        boolean b = sysClassService.updateClass(classDto);
        return R.success(b);
    }

    @GetMapping("/remove/{id}")
    @PreAuthorize("hasAuthority('user:class:remove')")
    @ApiOperation("删除Class")
    public R<Boolean> remove(@PathVariable @NotNull Long id) {
        boolean b = sysClassService.removeById(id);
        return R.success(b);
    }

    @GetMapping("/removeBatch/{ids}")
    @PreAuthorize("hasAuthority('user:class:remove')")
    @ApiOperation("删除class")
    public R<Boolean> removeBatch(@PathVariable @NotNull List<Long> ids) {
        boolean b = sysClassService.removeBatchByIds(ids);
        return R.success(b);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAuthority('user:class:add')")
    @ApiOperation("添加class")
    public R<Boolean> addClass(@RequestBody ClassDto classDto) {

        try {
            sysClassService.addClass(classDto);
        } catch (Exception e) {
            return R.success(false);
        }
        return R.success(true);
    }

}
