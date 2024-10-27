package com.anishan.user.mapper;

import com.anishan.api.domain.SysUser;
import com.anishan.user.domain.dto.PagedUserRoleQuery;
import com.anishan.user.domain.entity.SysUserRoleRelation;
import com.anishan.user.domain.vo.UserVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
* @author anishan
* @description 针对表【sys_user(用户表)】的数据库操作Mapper
* @createDate 2024-10-02 23:14:28
* @Entity com.anishan.api.entity.SysUser
*/
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    List<SysUser> selectByTeacherIdLimit(
            @Param("teacherId") Long teacherId,
            @Param("currentPage") Long currentPage,
            @Param("pageSize") Long pageSize);

    @Update("update sys_user set points = points + #{points} where user_id = #{userId}")
    int addPoints(@Param("userId") Long userId, @Param("points") BigDecimal points);

    List<UserVo> selectUserByUserRoleQuery(Page<SysUserRoleRelation> page, @Param("userQuery") PagedUserRoleQuery userQuery);
}




