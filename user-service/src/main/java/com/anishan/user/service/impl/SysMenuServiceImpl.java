package com.anishan.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.util.MysqlMappingUtils;
import com.anishan.commons.e.MenuType;
import com.anishan.user.domain.dto.MenuDto;
import com.anishan.user.domain.dto.MenuPagedQuery;
import com.anishan.api.domain.SysRole;
import com.anishan.user.domain.dto.RoleMenuRelationDto;
import com.anishan.user.domain.entity.SysRoleMenuRelation;
import com.anishan.user.domain.vo.MenuVo;
import com.anishan.user.domain.vo.TreedMenuVo;
import com.anishan.user.service.SysRoleMenuService;
import com.anishan.user.service.SysRoleService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.user.domain.entity.SysMenu;
import com.anishan.user.service.SysMenuService;
import com.anishan.user.mapper.SysMenuMapper;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu>
    implements SysMenuService{

    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuService sysRoleMenuService;
    private final SysRoleService sysRoleService;

    @Override
    public boolean isAllExist(List<Long> ids) {
        long count = this.count(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getMenuId, ids)
        );
        return count >= ids.size();
    }

    // dfs
    private void buildTreeMenuRecursion(Set<TreedMenuVo> menus, TreedMenuVo treeNode) {
        // If not MenuBar, exits
        if (treeNode == null || MenuType.Button.equals(treeNode.getType())) {
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
                .sorted(Comparator.comparingInt(o -> o.getMenu().getOrderNum()))
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
        if (CollectionUtil.isEmpty(menuIds)) {
            return List.of();
        }
        List<SysMenu> sysMenus = this.listByIds(menuIds);
        return BeanUtil.copyToList(sysMenus, MenuVo.class);
    }

    @Override
    public List<MenuVo> getMIMenusByRole(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<SysMenu> sysMenus = sysRoleMenuService.getMIMenuIdByRole(roleIds);
        return BeanUtil.copyToList(sysMenus, MenuVo.class);
    }

    @Override
    public List<TreedMenuVo> getTreedMenuByRole(List<Long> roleIds) {
        HashSet<MenuVo> menuVos = new HashSet<>(getMIMenusByRole(roleIds));

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

    @Override
    public boolean grant(List<RoleMenuRelationDto> relation) {
        List<SysRoleMenuRelation> collect = relation.stream().map(SysRoleMenuRelation::new).collect(Collectors.toList());


        List<Long> menuIds = collect.stream().map(SysRoleMenuRelation::getMenuId).collect(Collectors.toList());
        boolean menuExist = isAllExist(menuIds);

        if (!menuExist) {
            throw new RuntimeException("menu id 不存在");
        }


        List<Long> roleIds = collect.stream().map(SysRoleMenuRelation::getRoleId).collect(Collectors.toList());
        boolean roleExist = sysRoleService.isAllExist(roleIds);
        if (!roleExist) {
            throw new RuntimeException("role id 不存在");
        }

        return sysRoleMenuService.saveBatch(collect);
    }

    @Override
    public List<MenuVo> listRoleMenu(Long roleId) {
        return getMenusByRole(List.of(roleId));
    }

    @Override
    public List<TreedMenuVo> getAllTreedMenu() {
        List<MenuVo> vo = BeanUtil.copyToList(this.list(), MenuVo.class);
        HashSet<MenuVo> menuVos = new HashSet<>(vo);

        return buildTreeMenu(menuVos);
    }
}




