package com.anishan.user.entity.vo;

import com.anishan.commons.e.MenuType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    @JsonIgnore
    public MenuType getType() {
        return menu.getMenuType();
    }

    @JsonIgnore
    public Long getMenuId() {
        return this.menu.getMenuId();
    }

    @JsonIgnore
    public Long getParentId() {
        return this.menu.getParentId();
    }

    @JsonIgnore
    public boolean isRoot() {
        return this.menu.isRoot();
    }
}
