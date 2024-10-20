package com.anishan.user.service;

import com.anishan.commons.entity.dto.PagedQuery;
import com.anishan.commons.entity.vo.PagedResult;
import com.anishan.user.domain.dto.RoleDto;
import com.anishan.user.domain.dto.RolePagedQuery;
import com.anishan.api.domain.SysRole;
import com.anishan.user.domain.vo.RoleVo;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
* @author anishan
* @description 针对表【sys_role(角色信息表)】的数据库操作Service
* @createDate 2024-10-03 01:03:09
*/
public interface SysRoleService extends IService<SysRole> {

    boolean addRole(RoleDto roleDto);

    List<RoleVo> listRoleByIds(List<Long> ids);

    PagedResult<RoleVo> listRolesByPage(PagedQuery<SysRole> pagedQuery);

//    List<RoleVo> listRoleById(List<Long> ids);

    RoleVo getRoleById(@NotNull(message = "id为Null") Long id);

    PagedResult<RoleVo> queryRole(RolePagedQuery rolePagedQuery);

    boolean updateRole(RoleDto roleDto);
}
