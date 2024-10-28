package com.anishan.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.commons.util.MysqlMappingUtils;
import com.anishan.user.domain.dto.RoleDto;
import com.anishan.user.domain.dto.RolePagedQuery;
import com.anishan.user.domain.vo.RoleVo;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.api.domain.SysRole;
import com.anishan.user.service.SysRoleService;
import com.anishan.user.mapper.SysRoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

/**
* @author anishan
* @description 针对表【sys_role(角色信息表)】的数据库操作Service实现
* @createDate 2024-10-03 01:03:09
*/
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole>
    implements SysRoleService{
    private final SysRoleMapper sysRoleMapper;

    @Override
    public boolean existRole(Long id) {
        return this.exists(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleId, id)
        );
    }

    @Override
    public boolean isAllExist(List<Long> ids) {
        HashSet<Long> uniqueIds = new HashSet<>(ids);
        long count = this.count(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getRoleId, uniqueIds)
        );

        return count >= uniqueIds.size();
    }

    @Autowired
    public SysRoleServiceImpl(SysRoleMapper sysRoleMapper) {
        this.sysRoleMapper = sysRoleMapper;
    }

    @Override
    public RoleVo getRoleById(Long id) {
        SysRole sysRole = this.getById(id);
        return BeanUtil.copyProperties(sysRole, RoleVo.class);
    }

    @Override
    public  List<RoleVo> listRoleByIds(List<Long> ids) {
        List<SysRole> sysRoles = this.listByIds(ids);
        return BeanUtil.copyToList(sysRoles, RoleVo.class);
    }

    @Override
    public PagedResult<RoleVo> listRolesByPage(PagedQuery<SysRole> pagedQuery) {
        Page<SysRole> page = pagedQuery.page();
        page = this.page(page);
        return PagedResult.build(page, RoleVo.class);
    }


    @Override
    public PagedResult<RoleVo> queryRole(RolePagedQuery rolePagedQuery) {
        Page<SysRole> page = rolePagedQuery.page();
        Wrapper<SysRole> wrapper = rolePagedQuery.wrapper();
        page = this.page(page, wrapper);
        return PagedResult.build(page, RoleVo.class);
    }

    @Override
    public boolean updateRole(RoleDto roleDto) {

        LocalDateTime updateTime = MysqlMappingUtils.getUpdateTime(
                this,
                SysRole::getRoleId,
                roleDto.getRoleId(),
                SysRole::getUpdateTime);
        SysRole sysRole = BeanUtil.copyProperties(roleDto, SysRole.class);
        sysRole.setUpdateTime(updateTime);
        return this.updateById(sysRole);
    }




    @Override
    public boolean addRole(RoleDto roleDto) {
        SysRole sysRole = BeanUtil.copyProperties(roleDto, SysRole.class, "roleId");
        int insert = sysRoleMapper.insert(sysRole);
        return insert > 0;
    }


}




