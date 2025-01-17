package com.anishan.api.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MethodLog {

    String clazz() default "";

    String method() default "";

    String desc() default "";

}
