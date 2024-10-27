package com.anishan.user.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.anishan.api.domain.SysRole;
import com.anishan.api.domain.SysUser;
import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.user.domain.dto.UserRoleRelationDto;
import com.anishan.user.mapper.SysUserMapper;
import com.anishan.user.service.SysRoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.user.domain.entity.SysUserRoleRelation;
import com.anishan.user.service.SysUserRoleService;
import com.anishan.user.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
* @author anishan
* @description 针对表【sys_user_role(用户和角色关联表)】的数据库操作Service实现
* @createDate 2024-10-03 21:52:17
*/
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRoleRelation>
    implements SysUserRoleService{

    private final SysRoleService sysRoleService;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysUserMapper sysUserMapper;


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

    @Override
    public PagedResult<SysUserRoleRelation> getPagedByRoleId(PagedQuery<SysUserRoleRelation> query, Long id) {
        Page<SysUserRoleRelation> page = query.page();
        Page<SysUserRoleRelation> records = page(page, new LambdaUpdateWrapper<SysUserRoleRelation>()
                .eq(SysUserRoleRelation::getRoleId, id)
        );
        return PagedResult.fromPage(records);
    }

    @Override
    @Transactional
    public boolean removeBatch(List<UserRoleRelationDto> relations) {
        if (CollectionUtil.isEmpty(relations)) {
            return true;
        }
        return sysUserRoleMapper.deleteBatch(relations) > 0;
    }

    @Override
    public boolean grant(SysUserRoleRelation sysUserRoleRelation) {
        boolean role = sysRoleService.existRole(sysUserRoleRelation.getRoleId());
        if (!role) {
            throw new RuntimeException("角色不存在");
        }

        boolean exists = sysUserMapper.exists(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserId, sysUserRoleRelation.getUserId()));
        if (exists) {
            throw new RuntimeException("用户不存在");
        }



        return this.save(sysUserRoleRelation);
    }
}




