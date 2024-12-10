package com.anishan.commons.enumeration;

import com.fasterxml.jackson.annotation.JsonValue;

public interface Enumerator<T> {
    /**
     * 获取枚举码值
     * 序列化时采用改值
     */
    @JsonValue
    T value();

//    /**
//     * 获取枚举描述
//     * @return
//     */
//    String desc();
}