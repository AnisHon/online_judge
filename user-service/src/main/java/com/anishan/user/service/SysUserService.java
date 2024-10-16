package com.anishan.user.service;

import com.anishan.commons.entity.dto.PagedQuery;
import com.anishan.commons.entity.dto.UserDto;
import com.anishan.commons.entity.vo.PagedResult;
import com.anishan.user.domain.dto.SysUserDto;
import com.anishan.user.domain.dto.UserPagedQuery;
import com.anishan.api.entity.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.anishan.user.domain.vo.UserVo;

import java.util.List;

/**
* @author anishan
* @description 针对表【sys_user(用户表)】的数据库操作Service
* @createDate 2024-10-02 23:14:28
*/
public interface SysUserService extends IService<SysUser> {

    UserVo getUserById(Long id);

    List<UserVo> listUserById(List<String> ids);

    PagedResult<UserVo> listUsers(PagedQuery<SysUser> pagedQuery);

    PagedResult<UserVo> queryUser(UserPagedQuery userPagedQuery);

    boolean updateUser(UserDto userVo);

    void addUser(SysUserDto sysUserDto);

    SysUser getUserByUsernameOrEmail(String username);

    boolean existsUsername(String username);

    boolean existsEmail(String email);

    PagedResult<UserVo> queryUserWithin(UserPagedQuery userPagedQuery, List<Long> studentIds);

    PagedResult<UserVo> listStudentsOfTeacher(PagedQuery<SysUser> userPagedQuery);

    PagedResult<UserVo> listStudentsOfTeacher(Long userId, PagedQuery<SysUser> userPagedQuery);
}
