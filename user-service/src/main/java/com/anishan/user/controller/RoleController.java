package com.anishan.user.controller;

import com.anishan.commons.domain.R;
import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.enumeration.ValidationGroup;
import com.anishan.user.domain.dto.RoleDto;
import com.anishan.user.domain.dto.RolePagedQuery;
import com.anishan.api.domain.entity.SysRole;
import com.anishan.user.domain.dto.UserRoleRelationDto;
import com.anishan.user.domain.entity.SysUserRoleRelation;
import com.anishan.user.domain.vo.RoleVo;
import com.anishan.user.service.SysRoleService;
import com.anishan.user.service.SysUserRoleService;
import com.anishan.user.util.RoleUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/role")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RoleController {

    private final SysRoleService sysRoleService;
    private final SysUserRoleService sysUserRoleService;
    private final RoleUtil roleUtil;
    @DeleteMapping("/refresh")
    @PreAuthorize("hasAuthority('user:role:list')")
    @ApiOperation("刷新角色缓存")
    public R<String> refresh() {
        roleUtil.refresh();
        return R.success();
    }

    @GetMapping("/get/{id}")
    @PreAuthorize("hasAuthority('user:role:list')")
    @ApiOperation("通过id获取角色")
    public R<RoleVo> getMeById(@PathVariable("id") @NotNull(message = "id为Null") Long id) {
        RoleVo clazz = sysRoleService.getRoleById(id);
        return R.success(clazz);
    }


    @GetMapping("/{ids}")
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
    public R<PagedResult<RoleVo>> queryUser(@Validated  RolePagedQuery rolePagedQuery) {
        if (rolePagedQuery == null) {
            rolePagedQuery = new RolePagedQuery();
        }

        PagedResult<RoleVo> result = sysRoleService.queryRole(rolePagedQuery);

        return result.toR();
    }


    @PutMapping
    @PreAuthorize("hasAuthority('user:role:edit')")
    @ApiOperation("更新Role，不能更改roleId")
    public R<Boolean> update(@RequestBody RoleDto roleDto) {
        boolean b = sysRoleService.updateRole(roleDto);
        return R.success(b);
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAuthority('user:role:remove')")
    @ApiOperation("删除role")
    public R<Boolean> removeBatch(@PathVariable @NotNull List<Long> ids) {
        boolean b = sysRoleService.removeBatchByIds(ids);
        return R.success(b);
    }

    @PostMapping
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

    @PutMapping("/revoke")
    @PreAuthorize("hasAuthority('user:role:revoke')")
    @ApiOperation("撤销授予的角色")
    public R<Boolean> revoke(@RequestBody @Validated(ValidationGroup.Delete.class)UserRoleRelationDto relation) {
        boolean remove = sysUserRoleService.remove(new LambdaQueryWrapper<SysUserRoleRelation>()
                .eq(SysUserRoleRelation::getRoleId, relation.getRoleId())
                .eq(SysUserRoleRelation::getUserId, relation.getUserId())
        );
        return R.success(remove);
    }

    @PutMapping("/batchRevoke")
    @PreAuthorize("hasAuthority('user:role:revoke')")
    @ApiOperation("批量撤销授予的角色")
    public R<Boolean> batchRevoke(
            @RequestBody @Validated(ValidationGroup.Delete.class)List<UserRoleRelationDto> relations) {
        boolean remove = sysUserRoleService.removeBatch(relations);
        return R.success(remove);
    }

    @PostMapping("/grant")
    @PreAuthorize("hasAuthority('user:role:grant')")
    @ApiOperation("授予角色")
    public R<Boolean> grant(@RequestBody @Validated(ValidationGroup.Insert.class) UserRoleRelationDto relation) {
        SysUserRoleRelation sysUserRoleRelation = new SysUserRoleRelation(relation);
        boolean b = sysUserRoleService.grant(sysUserRoleRelation);

        return R.success(b);
    }

    @PostMapping("/batchGrant")
    @PreAuthorize("hasAuthority('user:role:grant')")
    @ApiOperation("批量授予角色")
    public R<Boolean> batchGrant(@RequestBody @Validated(ValidationGroup.Insert.class) List<UserRoleRelationDto> relations) {
        boolean b = sysUserRoleService.grantBatch(relations);
        return R.success(b);
    }

}
