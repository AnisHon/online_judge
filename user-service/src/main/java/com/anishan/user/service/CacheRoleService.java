package com.anishan.user.service;

import com.anishan.user.domain.vo.MenuVo;
import com.anishan.user.domain.vo.TreedMenuVo;

import java.util.List;

public interface CacheRoleService {
    void cacheMenus(Long roleId, List<MenuVo> menus);

    boolean isExist(Long roleId);

    List<MenuVo> getMenus(List<Long> roleIds);

    boolean isExistTreedMenu(Long roleId);

    void cacheTreedMenus(Long roleId, List<TreedMenuVo> menus);

    List<TreedMenuVo> getTreedMenus(List<Long> roleIds);

    void refresh();
}
