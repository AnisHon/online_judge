package com.anishan.user.domain.vo;

import com.anishan.commons.enumeration.MenuType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class TreedMenuVo {

    private MenuVo menu;
    private List<TreedMenuVo> children;

    public TreedMenuVo(MenuVo menu) {
        this.menu = menu;
    }

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
