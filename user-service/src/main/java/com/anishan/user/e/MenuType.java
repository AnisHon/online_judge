package com.anishan.user.e;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
public enum MenuType {

    Item("I"),
    MenuBar("M"),
    Button("B");

    private final String value;
    MenuType(String str) {
        value = str;
    }

    public static MenuType fromString(String str) {
        for (MenuType menuType : MenuType.values()) {
            if (menuType.value.equals(str)) {
                return menuType;
            }
        }
        return null;
    }


}
