package com.anishan.user.aspect;

import com.anishan.user.util.RoleUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * 由于缓存了Role和Menu所以这里使用切片进行刷新
 * 使用的方式是 先改后删 所以切片采用AfterReturning
 */

@Aspect
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class RoleMenuAspect {


    private final RoleUtil roleUtil;

    @Pointcut(
            "execution(* com.anishan.user.service.SysMenuService.grant(..)) ||" +
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
        roleUtil.refresh();

        log.info("---------------------------------");
        log.info("注意！！菜单权限或角色发生改变！刷新缓存");
        log.info("---------------------------------");
    }


}
