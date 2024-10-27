package com.anishan.user.mapper;

import com.anishan.user.domain.dto.UserRoleRelationDto;
import com.anishan.user.domain.entity.SysUserRoleRelation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author anishan
* @description 针对表【sys_user_role(用户和角色关联表)】的数据库操作Mapper
* @createDate 2024-10-03 21:52:17
* @Entity com.anishan.user.entity.po.SysUserRoleRelation
*/
public interface SysUserRoleMapper extends BaseMapper<SysUserRoleRelation> {

    int deleteBatch(@Param("relations") List<UserRoleRelationDto> relations);
}




