package com.anishan.user.controller;

import com.anishan.commons.entity.R;
import com.anishan.commons.entity.dto.PagedQuery;
import com.anishan.commons.entity.vo.PagedResult;
import com.anishan.commons.e.ValidationGroup;
import com.anishan.user.domain.dto.RoleDto;
import com.anishan.user.domain.dto.RolePagedQuery;
import com.anishan.user.domain.entity.SysRole;
import com.anishan.api.domain.RoleVo;
import com.anishan.user.service.SysRoleService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/role")
public class RoleController {

    private final SysRoleService sysRoleService;

    @Autowired
    public RoleController(SysRoleService sysRoleService) {
        this.sysRoleService = sysRoleService;
    }


    @GetMapping("/get/{id}")
    @PreAuthorize("hasAuthority('user:role:list')")
    @ApiOperation("通过id获取角色")
    public R<RoleVo> getMeById(@PathVariable("id") @NotNull(message = "id为Null") Long id) {
        RoleVo clazz = sysRoleService.getRoleById(id);
        return R.success(clazz);
    }

    @GetMapping("/list/{ids}")
    @PreAuthorize("hasAuthority('user:role:list')")
    @ApiOperation("通过多个id获取role，id之间用','隔开")
    public R<List<RoleVo>> listRole(@PathVariable("ids") List<Long> ids) {
        List<RoleVo> roles = sysRoleService.listRoleByIds(ids);
        return R.success(roles);
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('user:role:list')")
    @ApiOperation("分页获取role")
    public R<PagedResult<RoleVo>> listRoles(@Validated PagedQuery<SysRole> pagedQuery) {
        PagedResult<RoleVo> roleVoPagedResult = sysRoleService.listRolesByPage(pagedQuery);
        return roleVoPagedResult.toR();
    }

    @GetMapping("/query")
    @PreAuthorize("hasAuthority('user:role:list')")
    @ApiOperation("查询role")
    public R<PagedResult<RoleVo>> queryUser(RolePagedQuery rolePagedQuery) {
        if (rolePagedQuery == null) {
            rolePagedQuery = new RolePagedQuery();
        }

        PagedResult<RoleVo> result = sysRoleService.queryRole(rolePagedQuery);

        return result.toR();
    }


    @PostMapping("/update")
    @PreAuthorize("hasAuthority('user:role:edit')")
    @ApiOperation("更新Role，不能更改roleId")
    public R<Boolean> update(@RequestBody RoleDto roleDto) {
        boolean b = sysRoleService.updateRole(roleDto);
        return R.success(b);
    }

    @GetMapping("/remove/{id}")
    @PreAuthorize("hasAuthority('user:role:remove')")
    @ApiOperation("删除Role")
    public R<Boolean> remove(@PathVariable @NotNull Long id) {
        boolean b = sysRoleService.removeById(id);
        return R.success(b);
    }

    @GetMapping("/removeBatch/{ids}")
    @PreAuthorize("hasAuthority('user:role:remove')")
    @ApiOperation("删除role")
    public R<Boolean> removeBatch(@PathVariable @NotNull List<Long> ids) {
        boolean b = sysRoleService.removeBatchByIds(ids);
        return R.success(b);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAuthority('user:role:add')")
    @ApiOperation("添加role")
    public R<Boolean> addRole(@RequestBody @Validated(ValidationGroup.Update.class) RoleDto roleDto) {

        boolean b;
        try {
            b = sysRoleService.addRole(roleDto);
        } catch (Exception e) {
            return R.success(false);
        }
        return R.success(b);
    }
}
