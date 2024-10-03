package com.anishan.user.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.tree.Tree;
import com.anishan.user.e.MenuType;
import com.anishan.user.entity.po.SysMenu;
import com.anishan.user.entity.vo.MenuVo;
import com.anishan.user.entity.vo.TreedMenuVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author anishan
* @description 针对表【sys_menu(菜单权限表)】的数据库操作Service
* @createDate 2024-10-03 01:02:49
*/
public interface SysMenuService extends IService<SysMenu> {

    private void buildTreeMenu(Set<SysMenu> menus, TreedMenuVo treeNode) {
        if (treeNode == null || !MenuType.MenuBar.equals(treeNode.getType())) {
            return;
        }

        List<SysMenu> children = menus
                .stream()
                .filter(menu -> Objects.equals(menu.getParentId(), treeNode.getMenuId()))
                .collect(Collectors.toList());

        children.forEach(menus::remove);

        List<MenuVo> menuVos = BeanUtil.copyToList(children, MenuVo.class);
        treeNode.setChild(menuVos);

        for (TreedMenuVo menu : treeNode.getChildren()) {
            buildTreeMenu(menus, menu);
        }

    }

}
