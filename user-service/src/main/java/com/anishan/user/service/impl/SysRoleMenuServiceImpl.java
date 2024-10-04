package com.anishan.user.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.anishan.user.mapper.SysRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.user.entity.po.SysRoleMenuRelation;
import com.anishan.user.service.SysRoleMenuService;
import com.anishan.user.mapper.SysRoleMenuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author anishan
* @description 针对表【sys_role_menu(角色和菜单关联表)】的数据库操作Service实现
* @createDate 2024-10-04 21:52:51
*/
@Service
public class SysRoleMenuServiceImpl extends ServiceImpl<SysRoleMenuMapper, SysRoleMenuRelation>
    implements SysRoleMenuService{

    SysRoleMenuMapper sysRoleMenuMapper;

    @Autowired
    public SysRoleMenuServiceImpl(SysRoleMenuMapper sysRoleMenuMapper) {
        this.sysRoleMenuMapper = sysRoleMenuMapper;
    }

    @Override
    public List<Long> getMenuIdByRole(List<Long> roleIds) {
        if (CollectionUtil.isEmpty(roleIds)) {
            return new ArrayList<>();
        }
        return sysRoleMenuMapper.selectObjs(
                new LambdaQueryWrapper<SysRoleMenuRelation>()
                        .select(SysRoleMenuRelation::getMenuId)
                        .in(SysRoleMenuRelation::getRoleId, roleIds)
        );

    }

}




