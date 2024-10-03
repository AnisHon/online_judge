package com.anishan.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anishan.user.entity.po.SysUserRoleRelation;
import com.anishan.user.service.SysUserRoleService;
import com.anishan.user.mapper.SysUserRoleMapper;
import org.springframework.stereotype.Service;

/**
* @author anishan
* @description 针对表【sys_user_role(用户和角色关联表)】的数据库操作Service实现
* @createDate 2024-10-03 21:52:17
*/
@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRoleRelation>
    implements SysUserRoleService{

}




