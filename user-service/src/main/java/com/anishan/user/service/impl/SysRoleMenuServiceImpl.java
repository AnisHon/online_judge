package com.anishan.user.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.anishan.user.domain.dto.RoleMenuRelationDto;
import com.anishan.user.domain.entity.SysMenu;
import com.anishan.user.domain.vo.MenuVo;
import com.anishan.user.mapper.SysRoleMapper;
import com.anishan.user.service.SysRoleService;
import com.anishan.user.service.SysUserRoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.user.domain.entity.SysRoleMenuRelation;
import com.anishan.user.service.SysRoleMenuService;
import com.anishan.user.mapper.SysRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author anishan
* @description 针对表【sys_role_menu(角色和菜单关联表)】的数据库操作Service实现
* @createDate 2024-10-04 21:52:51
*/
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SysRoleMenuServiceImpl extends ServiceImpl<SysRoleMenuMapper, SysRoleMenuRelation>
    implements SysRoleMenuService{

    private final SysRoleMenuMapper sysRoleMenuMapper;

    @Override
    public List<Long> getMenuIdByRole(List<Long> roleIds) {
        if (CollectionUtil.isEmpty(roleIds)) {
            return List.of();
        }
        return sysRoleMenuMapper.selectObjs(
                new LambdaQueryWrapper<SysRoleMenuRelation>()
                        .select(SysRoleMenuRelation::getMenuId)
                        .in(SysRoleMenuRelation::getRoleId, roleIds)
        );

    }

    @Override
    public List<SysMenu> getMIMenuIdByRole(List<Long> roleIds) {
        return sysRoleMenuMapper.getMIMenuByRole(roleIds);
    }

    @Override
    public List<SysMenu> getAuthorityMenu(List<Long> roleIds) {
        return  sysRoleMenuMapper.getBMenuByRole(roleIds);
    }

    @Override
    public boolean removeBatch(List<RoleMenuRelationDto> relations) {
        if (CollectionUtil.isEmpty(relations)) {
            return true;
        }
        return sysRoleMenuMapper.deleteBatch(relations) > 0;
    }


}




