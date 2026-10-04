package com.anishan.user.aspect;

import com.anishan.user.service.CacheRoleService;
import com.anishan.user.service.SysUserService;
import com.anishan.api.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


/**
 * 由于缓存了Role和Menu所以这里使用切片进行刷新
 * 使用的方式是 先改后删 所以切片采用AfterReturning
 */

@Aspect
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class RoleMenuAspect {


    private final CacheRoleService cacheRoleService;
    private final AuthUtil authUtil;
    private final ObjectProvider<SysUserService> sysUserService;

    @Pointcut(
            "execution(* com.anishan.user.service.SysMenuService.grant(..)) ||" +
            "execution(* com.anishan.user.controller.RoleController.refresh(..)) ||" +
            "execution(* com.anishan.user.service.SysMenuService.add*(..)) ||" +
            "execution(* com.anishan.user.service.SysUserRoleService.grant*(..)) ||" +
            "execution(* com.anishan.user.service.SysUserRoleService.addRoleForUser(..)) ||" +
            "execution(* com.anishan.user.service.SysUserRoleService.remove*(..)) ||" +
            "execution(* com.anishan.user.service.SysRoleMenuService.remove*(..)) || " +
            "execution(* com.anishan.user.service.SysMenuService.remove*(..)) ||" +
            "execution(* com.anishan.user.service.SysMenuService.update*(..)) ||" +
            "execution(* com.anishan.user.service.SysRoleService.remove*(..)) ||" +
            "execution(* com.anishan.user.service.SysRoleService.update*(..))"
    )
    public void updateOrDeleteMethods() {
    }

    @AfterReturning("updateOrDeleteMethods()")
    public void beforeUpdateOrDelete() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { refreshPermissions(); }
            });
        } else {
            refreshPermissions();
        }
    }

    private void refreshPermissions() {
        cacheRoleService.refresh();
        SysUserService users = sysUserService.getObject();
        authUtil.refreshLoggedInUsers(id -> id == 0L ? users.getRootAccount() : users.getLoginUser(id));

        log.info("---------------------------------");
        log.info("注意！！菜单权限或角色发生改变！刷新缓存");
        log.info("---------------------------------");
    }




}
