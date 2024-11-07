package com.anishan.user.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.anishan.user.domain.vo.MenuVo;
import com.anishan.user.domain.vo.TreedMenuVo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RoleUtil {

    private final RedisTemplate<String, Object> redisTemplate;
    private final String userMenu = "user:menu:";
    private final String userTreedMenu = "user:treed-menu:";

    private String getMenuKey(Long roleId) {
        return userMenu + roleId;
    }
    private String getTreedMenuKey(Long roleId) {

        return userTreedMenu + roleId;
    }

    public void cacheMenus(Long roleId, List<MenuVo> menus) {
        String menuKey = getMenuKey(roleId);
        redisTemplate.opsForValue().set(menuKey, menus);
    }

    @SuppressWarnings("unchecked")
    public boolean isExist(Long roleId) {
        String menuKey = getMenuKey(roleId);
        boolean exist = Boolean.TRUE.equals(redisTemplate.hasKey(menuKey));
        if (exist) {
            Object o = redisTemplate.opsForValue().get(menuKey);


            List<MenuVo> menu = (List<MenuVo>) o;
            exist = !CollectionUtil.isEmpty(menu);
        }
        return exist;
    }


    @SuppressWarnings("unchecked")
    public List<MenuVo> getMenus(List<Long> roleIds) {
        List<MenuVo> menus = new ArrayList<>();
        for (Long roleId : roleIds) {
            String menuKey = getMenuKey(roleId);
            Object o = redisTemplate.opsForValue().get(menuKey);


            List<MenuVo> menu = (List<MenuVo>) o;
            if (menu != null) {
                menus.addAll(menu);
            } else {
                return null;
            }

        }
        return menus;
    }

    @SuppressWarnings("unchecked")
    public boolean isExistTreedMenu(Long roleId) {
        String menuKey = getTreedMenuKey(roleId);
        boolean exist = Boolean.TRUE.equals(redisTemplate.hasKey(menuKey));
        if (exist) {
            Object o = redisTemplate.opsForValue().get(menuKey);


            List<TreedMenuVo> menu = (List<TreedMenuVo>) o;
            exist = !CollectionUtil.isEmpty(menu);
        }
        return exist;
    }

    public void cacheTreedMenus(Long roleId, List<TreedMenuVo> menus) {
        String menuKey = getTreedMenuKey(roleId);
        redisTemplate.opsForValue().set(menuKey, menus);
    }

    @SuppressWarnings("unchecked")
    public List<TreedMenuVo> getTreedMenus(List<Long> roleIds) {
        List<TreedMenuVo> menus = new ArrayList<>();
        for (Long roleId : roleIds) {
            String menuKey = getTreedMenuKey(roleId);
            Object o = redisTemplate.opsForValue().get(menuKey);

            List<TreedMenuVo> menu = (List<TreedMenuVo>) o;
            if (menu != null) {
                menus.addAll(menu);
            } else {
                return null;
            }
        }
        return menus;
    }

    public void refresh() {
        Set<String> keys = redisTemplate.keys(userTreedMenu + '*');
        Set<String> keys1 = redisTemplate.keys(userMenu + '*');
        Collection<String> union = CollUtil.union(keys, keys1);
        redisTemplate.delete(union);
    }
}
