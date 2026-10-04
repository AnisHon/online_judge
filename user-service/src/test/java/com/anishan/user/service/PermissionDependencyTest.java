package com.anishan.user.service;

import com.anishan.api.util.AccountPolicy;
import com.anishan.commons.exception.BusinessException;
import com.anishan.user.domain.dto.RoleMenuRelationDto;
import com.anishan.user.domain.entity.SysMenu;
import com.anishan.user.mapper.SysMenuMapper;
import com.anishan.user.service.impl.SysMenuServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PermissionDependencyTest {
    private final SysRoleMenuService relations = mock(SysRoleMenuService.class);
    private final SysRoleService roles = mock(SysRoleService.class);
    private final SysMenuServiceImpl service = spy(new SysMenuServiceImpl(mock(SysMenuMapper.class), relations, roles));

    private void setup(String permission) {
        SysMenu menu = new SysMenu();
        menu.setMenuId(1202L);
        menu.setParentId(20L);
        menu.setPerms(permission);
        doReturn(menu).when(service).getById(1202L);
        doReturn(true).when(service).isAllExist(List.of(1202L));
        when(roles.isAllExist(List.of(7L))).thenReturn(true);
        when(relations.getMenuIdByRole(List.of(7L))).thenReturn(List.of());
        when(relations.saveBatch(anyList())).thenReturn(true);
    }

    @Test void denyMayBeGrantedWithoutBackendParentEvenForLegacyNesting() {
        setup(AccountPolicy.AVATAR_DENY);
        assertTrue(service.grant(List.of(new RoleMenuRelationDto(7L, 1202L))));
    }

    @Test void positiveManagementGrantMustIncludeItsRouteParent() {
        setup(AccountPolicy.AVATAR_RESET);
        assertThrows(BusinessException.class, () -> service.grant(List.of(new RoleMenuRelationDto(7L, 1202L))));
        verify(relations, never()).saveBatch(anyList());
    }
}
