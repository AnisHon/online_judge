package com.anishan.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.user.entity.po.SysUser;
import com.anishan.user.service.SysUserService;
import com.anishan.user.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

/**
* @author anishan
* @description 针对表【sys_user(用户表)】的数据库操作Service实现
* @createDate 2024-10-02 23:14:28
*/
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser>
    implements SysUserService{

}




