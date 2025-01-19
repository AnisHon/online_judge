package com.anishan.commons.enumeration;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

@Getter
public enum FolderType implements Enumerator<String>, IEnum<String> {

    DICTIONARY("D"),
    FILE("F"),
    MENU("M"),
    ;

    private final String value;
    FolderType(String value) {
        this.value = value;
    }
    @Override
    public String value() {
        return value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
