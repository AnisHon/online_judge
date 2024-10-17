package com.anishan.problem.controller;

import com.anishan.commons.e.ValidationGroup;
import com.anishan.commons.entity.R;

import com.anishan.problem.domain.dto.TagDto;
import com.anishan.problem.domain.vo.TagVo;
import com.anishan.problem.service.TagService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/tag")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TagController {

    private final TagService tagService;


    @GetMapping("/getAll")
    @ApiOperation("获取所有标签")
    public R<List<TagVo>> getAll() {

        List<TagVo> list = tagService.getAll();
        return R.success(list);
    }


    @PostMapping("/add")
    @PreAuthorize("hasAuthority('problem:tag:add')")
    @ApiOperation("添加标签")
    public R<Boolean> add(@RequestBody @Validated(ValidationGroup.Insert.class) TagDto tag) {
        boolean b = tagService.addTag(tag);
        return R.success(b);
    }



    @PostMapping("/update")
    @PreAuthorize("hasAuthority('problem:tag:update')")
    @ApiOperation("添加标签")
    public R<Boolean> update(@RequestBody @Validated(ValidationGroup.Update.class)  TagDto tag) {
        boolean b = tagService.updateTag(tag);
        return R.success(b);
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('problem:tag:delete')")
    @ApiOperation("添加标签")
    public R<Boolean> delete(@PathVariable @NotNull Long id) {
        boolean b = tagService.deleteTag(id);
        return R.success(b);
    }


}
