package com.anishan.problem.controller;

import com.anishan.api.annotation.EnableCache;
import com.anishan.commons.enumeration.ValidationGroup;
import com.anishan.commons.domain.R;

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
    @EnableCache(name = "get-tag")
    public R<List<TagVo>> getAll() {
        List<TagVo> list = tagService.getAll();
        return R.success(list);
    }

    @GetMapping("/problem/{id}")
    @ApiOperation("通过题目ID获取所有的标签")
    public R<List<TagVo>> getByProblemId(@PathVariable Long id) {
        List<TagVo> tagByProblemId = tagService.getTagByProblemId(id);
        return R.success(tagByProblemId);
    }


    @PostMapping
    @PreAuthorize("hasAuthority('problem:tag:add')")
    @ApiOperation("添加标签")
    public R<Boolean> add(@RequestBody @Validated(ValidationGroup.Insert.class) TagDto tag) {
        boolean b = tagService.addTag(tag);
        return R.success(b);
    }

    @PutMapping
    @PreAuthorize("hasAuthority('problem:tag:edit')")
    @ApiOperation("更改标签")
    public R<Boolean> update(@RequestBody @Validated(ValidationGroup.Update.class)  TagDto tag) {
        boolean b = tagService.updateTag(tag);
        return R.success(b);
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAuthority('problem:tag:delete')")
    @ApiOperation("批量删除标签")
    public R<Boolean> deleteBatch(@PathVariable @NotNull List<Long> ids) {
        boolean b = tagService.removeBatchByIds(ids);
        return R.success(b);
    }

}
