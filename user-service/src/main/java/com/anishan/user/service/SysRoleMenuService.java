package com.anishan.user.service;

import com.anishan.user.entity.po.SysRoleMenuRelation;
import com.baomidou.mybatisplus.extension.service.IService;

import java.awt.*;
import java.util.List;

/**
* @author anishan
* @description 针对表【sys_role_menu(角色和菜单关联表)】的数据库操作Service
* @createDate 2024-10-04 21:52:51
*/
public interface SysRoleMenuService extends IService<SysRoleMenuRelation> {
    List<Long> getMenuIdByRole(List<Long> roleIds);
}
