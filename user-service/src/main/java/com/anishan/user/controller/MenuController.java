package com.anishan.user.controller;

import com.anishan.commons.domain.R;
import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.e.ValidationGroup;
import com.anishan.user.domain.dto.MenuDto;
import com.anishan.user.domain.dto.MenuPagedQuery;
import com.anishan.user.domain.dto.RoleMenuRelationDto;
import com.anishan.user.domain.entity.SysMenu;
import com.anishan.user.domain.entity.SysRoleMenuRelation;
import com.anishan.user.domain.vo.MenuVo;
import com.anishan.user.service.SysMenuService;
import com.anishan.user.service.SysRoleMenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/menu")
@Api("权限 菜单相关接口")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MenuController {


    private final SysMenuService sysMenuService;
    private final SysRoleMenuService sysRoleMenuService;

    @GetMapping("/get/{id}")
    @PreAuthorize("hasAuthority('user:menu:list')")
    @ApiOperation("通过id获取菜单")
    public R<MenuVo> getMeById(@PathVariable("id") @NotNull(message = "id为Null") Long id) {
        MenuVo clazz = sysMenuService.getMenuById(id);
        return R.success(clazz);
    }

    @GetMapping("/list/{ids}")
    @PreAuthorize("hasAuthority('user:menu:list')")
    @ApiOperation("通过多个id获取menu，id之间用','隔开")
    public R<List<MenuVo>> listMenu(@PathVariable("ids") List<Long> ids) {
        List<MenuVo> menus = sysMenuService.listMenuById(ids);
        return R.success(menus);
    }

    @PostMapping("/page")
    @PreAuthorize("hasAuthority('user:menu:list')")
    @ApiOperation("分页获取menu")
    public R<PagedResult<MenuVo>> listMenus(@RequestBody @Validated PagedQuery<SysMenu> pagedQuery) {
        PagedResult<MenuVo> menuVoPagedResult = sysMenuService.listMenus(pagedQuery);
        return menuVoPagedResult.toR();
    }

    @PostMapping("/query")
    @PreAuthorize("hasAuthority('user:menu:list')")
    @ApiOperation("查询menu")
    public R<PagedResult<MenuVo>> queryMenu(@RequestBody MenuPagedQuery menuPagedQuery) {
        if (menuPagedQuery == null) {
            menuPagedQuery = new MenuPagedQuery();
        }

        PagedResult<MenuVo> result = sysMenuService.queryMenu(menuPagedQuery);

        return result.toR();
    }


    @PostMapping("/update")
    @PreAuthorize("hasAuthority('user:menu:edit')")
    @ApiOperation("更新Menu，不能更改menuId")
    public R<Boolean> update(@RequestBody @Validated(ValidationGroup.Update.class) MenuDto menuDto) {
        boolean b = sysMenuService.updateMenu(menuDto);
        return R.success(b);
    }

    @GetMapping("/remove/{id}")
    @PreAuthorize("hasAuthority('user:menu:remove')")
    @ApiOperation("删除Menu")
    public R<Boolean> remove(@PathVariable @NotNull Long id) {
        boolean b = sysMenuService.removeById(id);
        return R.success(b);
    }

    @GetMapping("/removeBatch/{ids}")
    @PreAuthorize("hasAuthority('user:menu:remove')")
    @ApiOperation("删除menu")
    public R<Boolean> removeBatch(@PathVariable @NotNull List<Long> ids) {
        boolean b = sysMenuService.removeBatchByIds(ids);
        return R.success(b);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAuthority('user:menu:add')")
    @ApiOperation("添加menu")
    public R<Boolean> addMenu(@RequestBody @Validated(ValidationGroup.Insert.class) MenuDto menuDto) {

        try {
            sysMenuService.addMenu(menuDto);
        } catch (Exception e) {
            return R.success(false);
        }
        return R.success(true);
    }

    @PostMapping("/revoke")
    @PreAuthorize("hasAuthority('user:menu:revoke')")
    @ApiOperation("添加menu")
    public R<Boolean> revoke(@RequestBody @Validated(ValidationGroup.Insert.class) RoleMenuRelationDto relation) {

        boolean b = sysRoleMenuService.remove(new LambdaQueryWrapper<SysRoleMenuRelation>()
                .eq(SysRoleMenuRelation::getMenuId, relation.getMenuId())
                .eq(SysRoleMenuRelation::getRoleId, relation.getRoleId())
        );
        return R.success(true);
    }

    @PostMapping("/batchRevoke")
    @PreAuthorize("hasAuthority('user:menu:revoke')")
    @ApiOperation("撤销权限")
    public R<Boolean> revoke(@RequestBody @Validated(ValidationGroup.Insert.class) List<RoleMenuRelationDto> relations) {

      sysRoleMenuService.removeBatch(relations);
        return R.success(true);
    }

    @PostMapping("/grant")
    @PreAuthorize("hasAuthority('user:menu:revoke')")
    @ApiOperation("授予权限")
    public R<Boolean> grant(@RequestBody @Validated List<RoleMenuRelationDto> relation) {

        boolean b = sysMenuService.grant(relation);


        return R.success(b);
    }

    @GetMapping("/listRoleMenu/{roleId}")
    @PreAuthorize("hasAuthority('user:menu:list')")
    @ApiOperation("列出角色权限")
    public R<List<MenuVo>> listRoleMenu(@PathVariable @NotNull Long roleId) {
        List<MenuVo> menus = sysMenuService.listRoleMenu(roleId);
        return R.success(menus);
    }


}
