package com.anishan.user.service.impl;

import com.anishan.api.entity.SysRole;
import com.anishan.user.service.SysRoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.user.entity.po.SysUserRoleRelation;
import com.anishan.user.service.SysUserRoleService;
import com.anishan.user.mapper.SysUserRoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author anishan
* @description 针对表【sys_user_role(用户和角色关联表)】的数据库操作Service实现
* @createDate 2024-10-03 21:52:17
*/
@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRoleRelation>
    implements SysUserRoleService{

    private final SysRoleService sysRoleService;
    private final SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    public SysUserRoleServiceImpl(SysRoleService sysRoleService, SysUserRoleMapper sysUserRoleMapper) {
        this.sysRoleService = sysRoleService;
        this.sysUserRoleMapper = sysUserRoleMapper;
    }

    @Override
    public List<Long> getRoleIdsByUserId(Long userId) {
        return this.listObjs(new LambdaQueryWrapper<SysUserRoleRelation>()
                        .select(SysUserRoleRelation::getRoleId)
                        .eq(SysUserRoleRelation::getUserId, userId)
        );
    }

    @Override
    public List<SysRole> getRolesByUserId(Long userId) {
        return sysRoleService.listByIds(getRoleIdsByUserId(userId));
    }

    @Override
    public void addRoleForUser(Long userId, Long roleId) {
        SysUserRoleRelation sysUserRoleRelation = new SysUserRoleRelation(userId, roleId);
        sysUserRoleMapper.insert(sysUserRoleRelation);
    }
}




