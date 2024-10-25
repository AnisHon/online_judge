package com.anishan.user.mapper;

import com.anishan.user.domain.entity.SysMenu;
import com.anishan.user.domain.entity.SysRoleMenuRelation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.awt.*;
import java.util.List;

/**
* @author anishan
* @description 针对表【sys_role_menu(角色和菜单关联表)】的数据库操作Mapper
* @createDate 2024-10-04 21:52:51
* @Entity com.anishan.user.entity.po.SysRoleMenuRelation
*/
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenuRelation> {

    List<SysMenu> getMIMenuByRole(List<Long> menuIds);

    List<SysMenu> getBMenuByRole(List<Long> menuIds);
}




