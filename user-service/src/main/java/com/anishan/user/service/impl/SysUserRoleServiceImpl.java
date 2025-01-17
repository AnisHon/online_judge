package com.anishan.user.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.anishan.api.domain.entity.SysRole;
import com.anishan.api.domain.entity.SysUser;
import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.util.ThrowUtil;
import com.anishan.user.domain.dto.UserRoleRelationDto;
import com.anishan.user.mapper.SysUserMapper;
import com.anishan.user.service.SysRoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.user.domain.entity.SysUserRoleRelation;
import com.anishan.user.service.SysUserRoleService;
import com.anishan.user.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
        List<Long> roleIdsByUserId = getRoleIdsByUserId(userId);
        if (CollectionUtil.isEmpty(roleIdsByUserId)) {
            return List.of();
        }
        return sysRoleService.listByIds(roleIdsByUserId);
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

        ThrowUtil.illegalArgument(!role, "角色不存在");

        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery(SysUser.class)
                .eq(SysUser::getUserId, sysUserRoleRelation.getUserId());

        boolean exists = Db.count(wrapper) > 0;

        ThrowUtil.illegalArgument(!exists, "用户不存在");

        return this.save(sysUserRoleRelation);
    }


    private boolean allUserExists(List<Long> userIds) {
        long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getUserId, userIds)
        );

        return count >= userIds.size();
    }

    @Override
    @Transactional
    public boolean grantBatch(List<UserRoleRelationDto> relations) {
        List<SysUserRoleRelation> sysRelations = relations
                .stream()
                .map(SysUserRoleRelation::new)
                .collect(Collectors.toList());
        List<Long> roleIds = sysRelations
                .stream()
                .map(SysUserRoleRelation::getRoleId)
                .collect(Collectors.toList());
        boolean roleExists = sysRoleService.isAllExist(roleIds);


        ThrowUtil.illegalArgument(!roleExists, "角色不存在");

        List<Long> userIds = sysRelations
                .stream()
                .map(SysUserRoleRelation::getUserId)
                .collect(Collectors.toList());


        boolean userExist = allUserExists(userIds);

        ThrowUtil.illegalArgument(!userExist, "用户不存在");

        return this.saveBatch(sysRelations);
    }
}




