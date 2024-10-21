package com.anishan.user.service;

import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.user.domain.dto.MenuDto;
import com.anishan.user.domain.dto.MenuPagedQuery;
import com.anishan.user.domain.entity.SysMenu;
import com.anishan.api.domain.SysRole;
import com.anishan.user.domain.vo.MenuVo;
import com.anishan.user.domain.vo.TreedMenuVo;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
* @author anishan
* @description 针对表【sys_menu(菜单权限表)】的数据库操作Service
* @createDate 2024-10-03 01:02:49
*/
public interface SysMenuService extends IService<SysMenu> {


    // 用于获取权限列表
    List<MenuVo> getMenusByRole(List<Long> roleIds);

    List<TreedMenuVo> getTreedMenuByRole(List<Long> roleIds);

    List<String> getAuthorities(List<Long> roleIds);

    List<String> getAuthoritiesByIds(List<Long> menuIds);

    List<String> getAuthorities_(List<SysRole> roleIds);

    MenuVo getMenuById(@NotNull(message = "id为Null") Long id);

    List<MenuVo> listMenuById(List<Long> ids);

    PagedResult<MenuVo> listMenus(PagedQuery<SysMenu> pagedQuery);

    PagedResult<MenuVo> queryMenu(MenuPagedQuery menuPagedQuery);

    boolean updateMenu(MenuDto menuDto);

    void addMenu(MenuDto menuDto);

}
