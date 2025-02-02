package com.anishan.user.controller;

import com.anishan.api.annotation.ControllerLog;
import com.anishan.commons.domain.R;
import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.enumeration.ValidationGroup;
import com.anishan.user.domain.dto.MenuDto;
import com.anishan.user.domain.dto.MenuPagedQuery;
import com.anishan.user.domain.dto.RoleMenuRelationDto;
import com.anishan.user.domain.entity.SysMenu;
import com.anishan.user.domain.entity.SysRoleMenuRelation;
import com.anishan.user.domain.vo.MenuVo;
import com.anishan.user.domain.vo.TreedMenuVo;
import com.anishan.user.service.SysMenuService;
import com.anishan.user.service.SysRoleMenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    @GetMapping("/{id}")
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

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('user:menu:list')")
    @ApiOperation("分页获取menu")
    public R<PagedResult<MenuVo>> listMenus(@Validated PagedQuery<SysMenu> pagedQuery) {
        PagedResult<MenuVo> menuVoPagedResult = sysMenuService.listMenus(pagedQuery);
        return menuVoPagedResult.toR();
    }

    @GetMapping("/query")
    @PreAuthorize("hasAuthority('user:menu:list')")
    @ApiOperation("查询menu")
    public R<PagedResult<MenuVo>> queryMenu(@Validated MenuPagedQuery menuPagedQuery) {
        if (menuPagedQuery == null) {
            menuPagedQuery = new MenuPagedQuery();
        }

        PagedResult<MenuVo> result = sysMenuService.queryMenu(menuPagedQuery);

        return result.toR();
    }

    @GetMapping("/treeMenus")
    @ApiOperation("获取所有菜单，以树状的形式返回")
    public R<List<TreedMenuVo>> menus() {
        return R.success(sysMenuService.getAllTreedMenu());
    }



    @PutMapping
    @PreAuthorize("hasAuthority('user:menu:edit')")
    @ApiOperation("更新Menu，不能更改menuId")
    @ControllerLog(api = "/menu", desc = "更新Menu")
    public R<Boolean> update(@RequestBody @Validated(ValidationGroup.Update.class) MenuDto menuDto) {
        boolean b = sysMenuService.updateMenu(menuDto);
        return R.success(b);
    }


    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAuthority('user:menu:remove')")
    @ApiOperation("删除menu")
    @ControllerLog(api = "/menu", desc = "删除Menu")
    public R<Boolean> removeBatch(@PathVariable @NotNull List<Long> ids) {
        boolean b = sysMenuService.removeBatchByIds(ids);
        return R.success(b);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:menu:add')")
    @ApiOperation("添加menu")
    @ControllerLog(api = "/menu", desc = "添加Menu")
    public R<Boolean> addMenu(@RequestBody @Validated(ValidationGroup.Insert.class) MenuDto menuDto) {

        try {
            sysMenuService.addMenu(menuDto);
        } catch (Exception e) {
            return R.success(false);
        }
        return R.success(true);
    }

    @PutMapping("/revoke")
    @PreAuthorize("hasAuthority('user:menu:revoke')")
    @ApiOperation("撤销权限")
    @ControllerLog(api = "/menu/revoke", desc = "撤销角色权限")
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
    @ControllerLog(api = "/menu/batchRevoke", desc = "撤销角色权限")
    public R<Boolean> revoke(@RequestBody @Validated(ValidationGroup.Insert.class) List<RoleMenuRelationDto> relations) {

      sysRoleMenuService.removeBatch(relations);
        return R.success(true);
    }

    @PostMapping("/grant")
    @PreAuthorize("hasAuthority('user:menu:grant')")
    @ApiOperation("授予权限")
    @ControllerLog(api = "/menu/grant", desc = "授予角色权限")
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
