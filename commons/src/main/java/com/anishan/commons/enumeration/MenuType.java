package com.anishan.commons.enumeration;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

@Getter
public enum MenuType implements IEnum<String>, Enumerator<String> {

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


    @Override
    public String value() {
        return getValue();
    }
}
