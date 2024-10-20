package com.anishan.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.anishan.commons.entity.dto.PagedQuery;
import com.anishan.commons.entity.vo.PagedResult;
import com.anishan.commons.util.MysqlMappingUtils;
import com.anishan.commons.e.MenuType;
import com.anishan.user.domain.dto.MenuDto;
import com.anishan.user.domain.dto.MenuPagedQuery;
import com.anishan.api.entity.SysRole;
import com.anishan.user.domain.vo.MenuVo;
import com.anishan.user.domain.vo.TreedMenuVo;
import com.anishan.user.service.SysRoleMenuService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.user.domain.entity.SysMenu;
import com.anishan.user.service.SysMenuService;
import com.anishan.user.mapper.SysMenuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author anishan
* @description 针对表【sys_menu(菜单权限表)】的数据库操作Service实现
* @createDate 2024-10-03 01:02:49
*/
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu>
    implements SysMenuService{

    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuService sysRoleMenuService;

    @Autowired
    public SysMenuServiceImpl(SysMenuMapper sysMenuMapper, SysRoleMenuService sysRoleMenuService) {
        this.sysMenuMapper = sysMenuMapper;
        this.sysRoleMenuService = sysRoleMenuService;
    }



    // dfs
    private void buildTreeMenuRecursion(Set<TreedMenuVo> menus, TreedMenuVo treeNode) {
        // If not MenuBar, exits
        if (treeNode == null || !MenuType.MenuBar.equals(treeNode.getType())) {
            return;
        }

        // search all children nodes
        List<TreedMenuVo> children = menus
                .stream()
                .filter(menu -> Objects.equals(menu.getParentId(), treeNode.getMenuId()))
                .sorted(Comparator.comparingInt(o -> o.getMenu().getOrderNum()))
                .collect(Collectors.toList());


        // set children
        treeNode.setChildren(children);

        children.forEach(menus::remove);

        // build TreeMenu for subMenu
        for (TreedMenuVo menu : treeNode.getChildren()) {
            buildTreeMenuRecursion(menus, menu);
        }

    }

    private List<TreedMenuVo> buildTreeMenu(Set<MenuVo> menus) {
        Set<TreedMenuVo> treedMenus = menus
                .stream()
                .map(TreedMenuVo::new)
                .collect(Collectors.toSet());

        List<TreedMenuVo> rootMenus = treedMenus
                .stream()
                .filter(TreedMenuVo::isRoot)
                .collect(Collectors.toList());

        rootMenus.forEach(treedMenus::remove);

        for (TreedMenuVo rootMenu : rootMenus) {
            buildTreeMenuRecursion(treedMenus, rootMenu);
        }
        return rootMenus;
    }

    @Override
    public List<MenuVo> getMenusByRole(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> menuIds = sysRoleMenuService.getMenuIdByRole(roleIds);
        List<SysMenu> sysMenus = this.listByIds(menuIds);
        return BeanUtil.copyToList(sysMenus, MenuVo.class);
    }

    @Override
    public List<TreedMenuVo> getTreedMenuByRole(List<Long> roleIds) {
        HashSet<MenuVo> menuVos = new HashSet<>(getMenusByRole(roleIds));

        return buildTreeMenu(menuVos);
    }

    @Override
    public List<String> getAuthorities(List<Long> roleIds) {
        if (CollectionUtil.isEmpty(roleIds)) {
            return new ArrayList<>();
        }

        List<Long> menuIds = sysRoleMenuService.getMenuIdByRole(roleIds);

        return getAuthoritiesByIds(menuIds);
    }

    @Override
    public List<String> getAuthoritiesByIds(List<Long> menuIds) {
        if (CollectionUtil.isEmpty(menuIds)) {
            return new ArrayList<>();
        }

        return this.listObjs(
                new LambdaQueryWrapper<SysMenu>()
                        .select(SysMenu::getPerms)
                        .in(SysMenu::getMenuId, menuIds));
    }

    @Override
    public List<String> getAuthorities_(List<SysRole> roleIds) {
        List<Long> collect = roleIds.stream().map(SysRole::getRoleId).collect(Collectors.toList());

        return getAuthorities(collect);
    }


    @Override
    public MenuVo getMenuById(Long id) {
        SysMenu byId = this.getById(id);
        return BeanUtil.copyProperties(byId, MenuVo.class);
    }

    @Override
    public List<MenuVo> listMenuById(List<Long> ids) {
        List<SysMenu> sysMenus = this.listByIds(ids);
        return BeanUtil.copyToList(sysMenus, MenuVo.class);
    }

    @Override
    public PagedResult<MenuVo> listMenus(PagedQuery<SysMenu> pagedQuery) {
        Page<SysMenu> page = pagedQuery.page();
        page = this.page(page);
        return PagedResult.build(page, MenuVo.class);
    }

    @Override
    public PagedResult<MenuVo> queryMenu(MenuPagedQuery menuPagedQuery) {
        Page<SysMenu> page = menuPagedQuery.page();
        Wrapper<SysMenu> wrapper = menuPagedQuery.wrapper();
        page = this.page(page, wrapper);
        return PagedResult.build(page, MenuVo.class);
    }

    @Override
    public boolean updateMenu(MenuDto menuDto) {
        SysMenu sysMenu = BeanUtil.copyProperties(menuDto, SysMenu.class);
        LocalDateTime updateTime = MysqlMappingUtils.getUpdateTime(
                this,
                SysMenu::getMenuId,
                sysMenu.getMenuId(),
                SysMenu::getUpdateTime);
        sysMenu.setUpdateTime(updateTime);
        return this.updateById(sysMenu);
    }

    @Override
    public void addMenu(MenuDto menuDto) {
        SysMenu sysMenu = BeanUtil.copyProperties(menuDto, SysMenu.class, "menuId");
        sysMenuMapper.insert(sysMenu);
    }
}




