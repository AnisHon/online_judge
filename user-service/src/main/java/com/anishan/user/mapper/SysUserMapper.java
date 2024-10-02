package com.anishan.user.mapper;

import com.anishan.user.entity.po.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author anishan
* @description 针对表【sys_user(用户表)】的数据库操作Mapper
* @createDate 2024-10-02 23:14:28
* @Entity com.anishan.user.entity.po.SysUser
*/
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

}




