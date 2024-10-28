package com.anishan.user.service;

import com.anishan.api.domain.SysRole;
import com.anishan.commons.domain.dto.PagedQuery;
import com.anishan.commons.domain.vo.PagedResult;
import com.anishan.user.domain.dto.UserRoleRelationDto;
import com.anishan.user.domain.entity.SysUserRoleRelation;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author anishan
* @description 针对表【sys_user_role(用户和角色关联表)】的数据库操作Service
* @createDate 2024-10-03 21:52:17
*/
public interface SysUserRoleService extends IService<SysUserRoleRelation> {


    List<Long> getRoleIdsByUserId(Long userId);

    List<SysRole> getRolesByUserId(Long userId);

    void addRoleForUser(Long userId, Long roleId);

    PagedResult<SysUserRoleRelation> getPagedByRoleId(PagedQuery<SysUserRoleRelation> query, Long id);

    boolean removeBatch(List<UserRoleRelationDto> relations);

    boolean grant(SysUserRoleRelation sysUserRoleRelation);

    boolean grantBatch(List<UserRoleRelationDto> relations);
}
