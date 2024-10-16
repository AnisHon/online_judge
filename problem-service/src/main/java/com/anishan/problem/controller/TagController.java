package com.anishan.problem.controller;

import com.anishan.commons.entity.R;
import com.anishan.commons.entity.dto.PagedQuery;
import com.anishan.commons.entity.vo.PagedResult;
import com.anishan.commons.e.ValidationGroup;

import com.anishan.problem.entity.dto.SysTagDto;
import com.anishan.problem.entity.po.SysTag;
import com.anishan.problem.entity.vo.SysTagVo;
import com.anishan.problem.service.SysTagService;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/tag")
@ApiModel("标签API，增删改查，可以代替难度和用于其他分类")
public class TagController {


    private final SysTagService sysTagService;
    @Autowired
    public TagController(SysTagService sysTagService) {
        this.sysTagService = sysTagService;
    }


    @GetMapping("/get/{id}")
    @PreAuthorize("hasAuthority('user:tag:list')")
    @ApiOperation("通过id获取Tag")
    public R<SysTagVo> getTagById(@PathVariable("id") @NotNull(message = "id为Null") Long id) {
        SysTagVo clazz = sysTagService.getTagById(id);
        return R.success(clazz);
    }

    @GetMapping("/list/{ids}")
    @PreAuthorize("hasAuthority('user:tag:list')")
    @ApiOperation("通过多个id获取tag，id之间用','隔开 要求有user:user:get")
    public R<List<SysTagVo>> listTag(@PathVariable("ids") List<Long> ids) {
        List<SysTagVo> tags = sysTagService.listTagById(ids);
        return R.success(tags);
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('user:tag:list')")
    @ApiOperation("分页获取tag")
    public R<PagedResult<SysTagVo>> listTags(@Validated PagedQuery<SysTag> pagedQuery) {
        PagedResult<SysTagVo> tagVoPagedResult = sysTagService.listTages(pagedQuery);
        return tagVoPagedResult.toR();
    }



    @PostMapping("/update")
    @PreAuthorize("hasAuthority('user:tag:edit')")
    @ApiOperation("更新Tag，不能更改tagId")
    public R<Boolean> update(@RequestBody @Validated({ValidationGroup.Update.class}) SysTagDto tagDto) {
        boolean b = sysTagService.updateTag(tagDto);
        return R.success(b);
    }

    @GetMapping("/remove/{id}")
    @PreAuthorize("hasAuthority('user:tag:remove')")
    @ApiOperation("删除Tag")
    public R<Boolean> remove(@PathVariable @NotNull Long id) {
        boolean b = sysTagService.removeById(id);
        return R.success(b);
    }

    @GetMapping("/removeBatch/{ids}")
    @PreAuthorize("hasAuthority('user:tag:remove')")
    @ApiOperation("删除tag")
    public R<Boolean> removeBatch(@PathVariable @NotNull List<Long> ids) {
        boolean b = sysTagService.removeBatchByIds(ids);
        return R.success(b);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAuthority('user:tag:add')")
    @ApiOperation("添加tag")
    public R<Boolean> addTag(@RequestBody SysTagDto tagDto) {

        boolean b;
        try {
            b = sysTagService.addTag(tagDto);
        } catch (DuplicateKeyException e) {
            return R.conflict("name冲突");
        } catch (Exception e) {
            return R.badRequest("错误");
        }
        return R.success(b);
    }

}
