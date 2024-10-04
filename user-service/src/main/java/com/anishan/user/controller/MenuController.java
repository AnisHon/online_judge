package com.anishan.user.controller;

import com.anishan.commons.entity.R;
import com.anishan.commons.entity.dto.PagedQuery;
import com.anishan.commons.entity.vo.PagedResult;
import com.anishan.user.e.ValidationGroup;
import com.anishan.user.entity.dto.MenuDto;
import com.anishan.user.entity.dto.MenuPagedQuery;
import com.anishan.user.entity.po.SysMenu;
import com.anishan.user.entity.vo.MenuVo;
import com.anishan.user.service.SysMenuService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/menu")
public class MenuController {


    SysMenuService sysMenuService;
    @Autowired
    public MenuController(SysMenuService sysMenuService) {
        this.sysMenuService = sysMenuService;
    }


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
    public R<PagedResult<MenuVo>> queryUser(MenuPagedQuery menuPagedQuery) {
        if (menuPagedQuery == null) {
            menuPagedQuery = new MenuPagedQuery();
        }

        PagedResult<MenuVo> result = sysMenuService.queryMenu(menuPagedQuery);

        return result.toR();
    }


    @PostMapping("/update")
    @PreAuthorize("hasAuthority('user:menu:edit')")
    @ApiOperation("更新Menu，不能更改menuId")
    public R<Boolean> update(@RequestBody MenuDto menuDto) {
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
    public R<Boolean> addMenu(@RequestBody @Validated(ValidationGroup.Update.class) MenuDto menuDto) {

        try {
            sysMenuService.addMenu(menuDto);
        } catch (Exception e) {
            return R.success(false);
        }
        return R.success(true);
    }



}
