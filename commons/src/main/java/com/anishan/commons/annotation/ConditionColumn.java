package com.anishan.commons.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 默认like用like排序，一般是eq和like
 * name就是如果列名称与变量名不一致用这个
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConditionColumn {

    String value() default "like";
    String name() default "";


}
