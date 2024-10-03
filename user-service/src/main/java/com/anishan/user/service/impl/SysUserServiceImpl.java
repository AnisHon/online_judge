package com.anishan.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.anishan.commons.entity.dto.PagedQuery;
import com.anishan.commons.entity.dto.UserDto;
import com.anishan.commons.entity.vo.PagedResult;
import com.anishan.user.entity.dto.UserPagedQuery;
import com.anishan.user.entity.vo.UserVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.user.entity.po.SysUser;
import com.anishan.user.service.SysUserService;
import com.anishan.user.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author anishan
* @description 针对表【sys_user(用户表)】的数据库操作Service实现
* @createDate 2024-10-02 23:14:28
*/
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser>
    implements SysUserService{

    SysUserMapper sysUserMapper;

    @Autowired
    public SysUserServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public UserVo getUserById(Long id) {
        SysUser sysUser = this.getById(id);
        return BeanUtil.copyProperties(sysUser, UserVo.class);
    }

    @Override
    public List<UserVo> listUserById(List<String> ids) {
        List<SysUser> sysUsers = sysUserMapper.selectBatchIds(ids);
        return BeanUtil.copyToList(sysUsers, UserVo.class);
    }

    @Override
    public PagedResult<UserVo> listUsers(PagedQuery<SysUser> pagedQuery) {

        Page<SysUser> page = pagedQuery.page();
        page = this.page(page);
        return PagedResult.build(page, UserVo.class);
    }

    @Override
    public PagedResult<UserVo> queryUser(UserPagedQuery userPagedQuery) {
        LambdaQueryWrapper<SysUser> wrapper = userPagedQuery.wrapper();
        Page<SysUser> page = userPagedQuery.page();
        page = page(page, wrapper);
        return PagedResult.build(page, UserVo.class);
    }

    @Override
    public void updateUser(UserDto userDto) {
        SysUser sysUser = BeanUtil.copyProperties(userDto, SysUser.class);
        this.updateById(sysUser);
    }



}




