package com.anishan.api.annotation;

import java.lang.annotation.*;

/**
 * 简单缓存，只通过参数对象摘要值做缓存
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableCache {

    /**
     * 默认五分钟
     */
    long expire() default 5 * 60 * 1000;

    /**
     * 缓存基础名
     */
    String name();

}
