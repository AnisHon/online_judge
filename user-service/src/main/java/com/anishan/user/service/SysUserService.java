package com.anishan.user.service;

import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.commons.domain.dto.UserDto;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.user.domain.dto.*;
import com.anishan.api.domain.entity.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.anishan.api.client.user.domain.vo.UserVo;

import java.math.BigDecimal;
import java.util.List;

/**
* @author anishan
* @description 针对表【sys_user(用户表)】的数据库操作Service
* @createDate 2024-10-02 23:14:28
*/
public interface SysUserService extends IService<SysUser> {

    UserVo getUserById(Long id);

    boolean addPoint(Long userId, BigDecimal point);

    List<UserVo> listUserById(List<Long> ids);

    PagedResult<UserVo> listUsers(PagedQuery<SysUser> pagedQuery);

    PagedResult<UserVo> queryUser(UserPagedQuery userPagedQuery);

    boolean updateUser(SysUser user);

    boolean updateUser(UserDto userVo);

    void addUser(SysUserDto sysUserDto);

    SysUser getUserByUsernameOrEmail(String username);

    boolean existsUsername(String username);

    boolean existsEmail(String email);

    boolean existsId(Long id);

    PagedResult<UserVo> queryUserWithin(UserPagedQuery userPagedQuery, List<Long> studentIds);

    PagedResult<UserVo> listStudentsOfTeacher(PagedQuery<SysUser> userPagedQuery);

    PagedResult<UserVo> listStudentsOfTeacher(Long userId, PagedQuery<SysUser> userPagedQuery);

    PagedResult<UserVo> getUserByRoleId(PagedUserRoleQuery userQuery);

    List<UserVo> rank(Integer limit);

    boolean changeInfo(SysUserInfoDto sysUserDto, Long userId);

    UserPoint getPoint(Long userId);
}
