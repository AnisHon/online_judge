package com.anishan.user.entity.vo;

import com.anishan.user.e.MenuType;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@RequiredArgsConstructor
public class TreedMenuVo {

    private final MenuVo menu;
    private List<TreedMenuVo> children;

    public void setChild(List<MenuVo> children) {
        this.children = children
                .stream()
                .map(TreedMenuVo::new)
                .collect(Collectors.toList());
    }

    public void addChild(TreedMenuVo treedMenuVo) {
        if (children == null) {
            children = new ArrayList<>();
        }
        Optional.ofNullable(treedMenuVo)
                .ifPresent(treedMenuVo1 -> children.add(treedMenuVo1));
    }

    public MenuType getType() {
        return MenuType.fromString(menu.getMenuType());
    }

    public Long getMenuId() {
        return this.menu.getMenuId();
    }

}
